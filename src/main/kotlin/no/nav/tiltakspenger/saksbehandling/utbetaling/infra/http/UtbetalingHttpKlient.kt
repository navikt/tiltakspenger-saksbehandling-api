package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.flatten
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeHenteUtbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.utsjekk.kontrakter.iverksett.IverksettStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val log = KotlinLogging.logger {}

/**
 * https://navikt.github.io/utsjekk-docs/
 */
class UtbetalingHttpKlient(
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 5.seconds,
    private val timeout: Duration = 15.seconds,
    private val clock: Clock,
) : Utbetalingsklient {
    private val simuleringTimeout: Duration = 45.seconds

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val simuleringClient =
        HttpClient
            .newBuilder()
            .connectTimeout(simuleringTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val iverksettUri = URI.create("$baseUrl/api/iverksetting/v2")

    override suspend fun iverksett(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        return withContext(Dispatchers.IO) {
            Either.catch {
                val token = getToken()
                val jsonPayload = utbetaling.toUtbetalingRequestDTO(forrigeUtbetalingJson)
                val request = createIverksettRequest(correlationId, jsonPayload, token.token)

                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val jsonResponse = httpResponse.body()
                mapIverksettStatus(
                    status = httpResponse.statusCode(),
                    utbetaling = utbetaling,
                    request = jsonPayload,
                    response = jsonResponse,
                    token = token,
                )
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(it) {
                    "Ukjent feil ved utsjekk for utbetaling ${utbetaling.id}. Saksnummer ${utbetaling.saksnummer}, sakId: ${utbetaling.sakId}"
                }
                KunneIkkeUtbetale()
            }.flatten()
        }
    }

    private fun createIverksettRequest(
        correlationId: CorrelationId,
        jsonPayload: String,
        token: String,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(iverksettUri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            // Dette er kun for vår del, open telemetry vil kunne være et alternativ. Slack tråd: https://nav-it.slack.com/archives/C06SJTR2X3L/p1724072054018589
            .header("Nav-Call-Id", correlationId.value)
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }

    /**
     * Logger alle feil(lefts), så det trengs ikke gjøres av service/domenet.
     * Nåværende versjon: https://helved-docs.intern.dev.nav.no/v2/doc/status
     * Neste versjon: https://helved-docs.intern.dev.nav.no/v3/doc/sjekk_status_pa_en_utbetaling
     */
    override suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<KunneIkkeHenteUtbetalingsstatus, Utbetalingsstatus> {
        return withContext(Dispatchers.IO) {
            val (utbetalingId, sakId, saksnummer) = utbetaling
            val path = "$baseUrl/api/iverksetting/${saksnummer.verdi}/${utbetalingId.uuidPart()}/status"
            Either.catch {
                val token = getToken().token
                val request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(path))
                    .timeout(timeout.toJavaDuration())
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .GET()
                    .build()

                val requestHeaders = request.headers().map().filterKeys { it != "Authorization" }
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val httpResponseBody = httpResponse.body()
                val status = httpResponse.statusCode()
                val responseHeaders = httpResponse.headers().map()
                if (status != 200) {
                    log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Feil ved henting av utbetalingsstatus. Status var ulik 200. Se sikkerlogg for mer kontekst. utbetalingId: $utbetalingId, saksnummer: $saksnummer, sakId: $sakId, path: $path, status: $status" }
                    Sikkerlogg.error { "Feil ved henting av utbetalingsstatus. Status var ulik 200. utbetalingId: $utbetalingId, saksnummer: $saksnummer, sakId: $sakId, httpResponseBody: $httpResponseBody, path: $path, status: $status, requestHeaders: $requestHeaders, responseHeaders: $responseHeaders" }
                    return@catch KunneIkkeHenteUtbetalingsstatus.left()
                }
                Either.catch {
                    when (deserialize<IverksettStatus?>(httpResponseBody)) {
                        IverksettStatus.SENDT_TIL_OPPDRAG -> Utbetalingsstatus.SendtTilOppdrag.right()
                        IverksettStatus.FEILET_MOT_OPPDRAG -> Utbetalingsstatus.FeiletMotOppdrag.right()
                        IverksettStatus.OK -> Utbetalingsstatus.Ok.right()
                        IverksettStatus.IKKE_PÅBEGYNT -> Utbetalingsstatus.IkkePåbegynt.right()
                        IverksettStatus.OK_UTEN_UTBETALING -> Utbetalingsstatus.OkUtenUtbetaling.right()
                        null -> {
                            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Respons fra statusapiet til helved var null. Dette forventer vi ikke. utbetalingId: $utbetalingId, saksnummer: $saksnummer, sakId: $sakId, path: $path, status: $status" }
                            KunneIkkeHenteUtbetalingsstatus.left()
                        }
                    }
                }.getOrElse {
                    log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Feil ved deserialisering av utbetalingsstatus. Se sikkerlogg for mer kontekst. utbetalingId: $utbetalingId, saksnummer: $saksnummer, sakId: $sakId, path: $path, status: $status" }
                    Sikkerlogg.error(it) { "Feil ved deserialisering av utbetalingsstatus. utbetalingId: $utbetalingId, saksnummer: $saksnummer, sakId: $sakId, jsonResponse: $httpResponseBody, path: $path, status: $status" }
                    KunneIkkeHenteUtbetalingsstatus.left()
                }
            }.mapLeft {
                log.error(it) { "Ukjent feil ved henting av utbetalingsstatus. utbetalingId: $utbetalingId, saksnummer: $saksnummer, sakId: $sakId, path: $path" }
                KunneIkkeHenteUtbetalingsstatus
            }.flatten()
        }
    }

    /**
     * Logger alle feil(lefts), så det trengs ikke gjøres av service/domenet.
     * Nåværende versjon: https://helved-docs.intern.dev.nav.no/v2/doc/simulering
     */
    override suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        saksbehandler: String,
        beregning: Beregning,
        brukersNavkontor: Navkontor,
        kanSendeInnHelgForMeldekort: Boolean,
        forrigeUtbetalingJson: String?,
        forrigeUtbetalingId: UtbetalingId?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return withContext(Dispatchers.IO) {
            val sakId = sakId
            val saksnummer = saksnummer
            val behandlingId = behandlingId
            val path = "$baseUrl/api/simulering/v2"
            val jsonPayload = toSimuleringRequest(
                saksnummer = saksnummer,
                behandlingId = behandlingId,
                fnr = fnr,
                saksbehandler = saksbehandler,
                beregning = beregning,
                brukersNavkontor = brukersNavkontor,
                forrigeUtbetalingJson = forrigeUtbetalingJson,
                forrigeUtbetalingId = forrigeUtbetalingId,
            )
            Either.catch {
                val token = getToken().token
                val request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(path))
                    .timeout(simuleringTimeout.toJavaDuration())
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

                val requestHeaders = request.headers().map().filterKeys { it != "Authorization" }
                val httpResponse = simuleringClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val httpResponseBody = httpResponse.body()
                val status = httpResponse.statusCode()
                val responseHeaders = httpResponse.headers().map()
                if (status == 503) {
                    return@catch KunneIkkeSimulere.Stengt.left()
                }
                if (status == 204) {
                    return@catch SimuleringMedMetadata(
                        simulering = Simulering.IngenEndring(LocalDateTime.now(clock)),
                        httpResponseBody,
                    ).right()
                }
                if (status != 200) {
                    log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Feil ved simulering. Status var ulik 200. Se sikkerlogg for mer kontekst. behandlingId: $behandlingId, saksnummer: $saksnummer, sakId: $sakId, path: $path, status: $status" }
                    Sikkerlogg.error { "Feil ved simulering. Status var ulik 200. behandlingId: $behandlingId, saksnummer: $saksnummer, sakId: $sakId, httpResponseBody: $httpResponseBody, path: $path, status: $status, requestHeaders: $requestHeaders, responseHeaders: $responseHeaders" }
                    return@catch KunneIkkeSimulere.UkjentFeil.left()
                }
                Either.catch {
                    SimuleringMedMetadata(
                        httpResponseBody.toSimuleringFraHelvedResponse(
                            meldeperiodeKjeder = meldeperiodeKjeder,
                            clock = clock,
                        ),
                        httpResponseBody,
                    ).right()
                }.getOrElse {
                    log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Feil ved deserialisering av simulering. Se sikkerlogg for mer kontekst. behandlingId: $behandlingId, saksnummer: $saksnummer, sakId: $sakId, path: $path, status: $status" }
                    Sikkerlogg.error(it) { "Feil ved deserialisering av simulering. behandlingId: $behandlingId, saksnummer: $saksnummer, sakId: $sakId, jsonResponse: $httpResponseBody, path: $path, status: $status" }
                    KunneIkkeSimulere.UkjentFeil.left()
                }
            }.mapLeft {
                log.error(it) { "Feil ved simulering. behandlingId: $behandlingId, saksnummer: $saksnummer, sakId: $sakId, path: $path" }

                when (it) {
                    is HttpTimeoutException -> KunneIkkeSimulere.Timeout
                    else -> KunneIkkeSimulere.UkjentFeil
                }
            }.flatten()
        }
    }
}

private fun mapIverksettStatus(
    status: Int,
    utbetaling: VedtattUtbetaling,
    request: String,
    response: String,
    token: AccessToken,
): Either<KunneIkkeUtbetale, SendtUtbetaling> {
    val utbetalingId = utbetaling.id

    when (status) {
        202 -> {
            log.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "202 Accepted fra helved utsjekk for, utbetaling $utbetalingId. Response: $response. Se sikkerlogg for mer kontekst."
            }
            Sikkerlogg.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "202 Accepted fra helved utsjekk for, utbetaling $utbetalingId. Response: $response. Request = $request"
            }
            return SendtUtbetaling(
                request = request,
                response = response,
                responseStatus = status,
            ).right()
        }

        400 -> {
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "400 Bad Request fra helved utsjekk, for utbetaling $utbetalingId. Denne vil bli prøvd på nytt. Response: $response. Se sikkerlogg for mer kontekst."
            }
            Sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "400 Bad Request fra helved utsjekk, for utbetaling $utbetalingId. Denne vil bli prøvd på nytt. Response: $response. Request = $request"
            }
            return KunneIkkeUtbetale(
                request = request,
                response = response,
                responseStatus = status,
            ).left()
        }

        401, 403 -> {
            token.invaliderCache()
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "$status fra helved utsjekk, for utbetaling $utbetalingId. Denne vil bli prøvd på nytt. Response: $response. Se sikkerlogg for mer kontekst."
            }
            Sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "$status fra helved utsjekk, for utbetaling $utbetalingId. Denne vil bli prøvd på nytt. Response: $response. Request = $request"
            }
            return KunneIkkeUtbetale(
                request = request,
                response = response,
                responseStatus = status,
            ).left()
        }

        409 -> {
            // TODO post-mvp jah: På sikt er dette en litt skjør sjekk som kan føre til at vi må endre denne sjekken dersom helved forandrer meldingen. Vi har bestilt et ønske fra helved om at vi får en json-respons med en kontraktsfestet kode, evt. at de garanterer at 409 kun brukes til dedupformål.
            if (response.contains("Iverksettingen er allerede mottatt")) {
                log.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetaling $utbetalingId. Vi antar vi har sendt samme melding tidligere og behandler denne på samme måte som 202 Response: $response. Se sikkerlogg for mer kontekst."
                }
                Sikkerlogg.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetaling $utbetalingId. Vi antar vi har sendt samme melding tidligere og behandler denne på samme måte som 202 Response: $response. Request = $request"
                }
                return SendtUtbetaling(
                    request = request,
                    response = response,
                    responseStatus = status,
                ).right()
            } else {
                log.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetaling $utbetalingId. Vi forventet responsen 'Iverksettingen er allerede mottatt', men fikk $response. Se sikkerlogg for mer kontekst."
                }
                Sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetaling $utbetalingId. Vi forventet responsen 'Iverksettingen er allerede mottatt', men fikk $response. Request = $request"
                }
                return KunneIkkeUtbetale(
                    request = request,
                    response = response,
                    responseStatus = status,
                ).left()
            }
        }

        else -> {
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "Ukjent feil fra helved utsjekk, for utbetaling $utbetalingId. Denne vil bli prøvd på nytt. Statuskode: $status, response: $response. Se sikkerlogg for mer kontekst."
            }
            Sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "Ukjent feil fra helved utsjekk, for utbetaling $utbetalingId. Denne vil bli prøvd på nytt. Statuskode: $status, response: $response. Request = $request"
            }
            return KunneIkkeUtbetale(
                request = request,
                response = response,
                responseStatus = status,
            ).left()
        }
    }
}
