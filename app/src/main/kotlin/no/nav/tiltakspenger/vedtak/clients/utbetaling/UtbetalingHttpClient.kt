package no.nav.tiltakspenger.vedtak.clients.utbetaling

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.vedtak.felles.sikkerlogg
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.UtbetalingGateway
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Utbetalingsvedtak
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val log = KotlinLogging.logger {}

/**
 * https://navikt.github.io/utsjekk-docs/
 */
class UtbetalingHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 1.seconds,
) : UtbetalingGateway {

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val uri = URI.create("$baseUrl/api/iverksetting/v2")

    override suspend fun iverksett(
        vedtak: Utbetalingsvedtak,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> =
        withContext(Dispatchers.IO) {
            Either.catch {
                val token = getToken()
                val jsonPayload = vedtak.toDTO(forrigeUtbetalingJson)
                val request = createRequest(correlationId, jsonPayload, token.token)

                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val jsonResponse = httpResponse.body()
                mapStatus(
                    status = httpResponse.statusCode(),
                    vedtak = vedtak,
                    request = jsonPayload,
                    response = jsonResponse,
                    token = token,
                )
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Ukjent feil ved utsjekk for utbetalingsvedtak ${vedtak.id}. Saksnummer ${vedtak.saksnummer}, sakId: ${vedtak.sakId}" }
                sikkerlogg.error(it) { "Ukjent feil ved utsjekk for utbetalingsvedtak ${vedtak.id}. Saksnummer ${vedtak.saksnummer}, sakId: ${vedtak.sakId}" }
                KunneIkkeUtbetale()
            }.flatten()
        }

    private fun createRequest(
        correlationId: CorrelationId,
        jsonPayload: String,
        token: String,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            // Dette er kun for vår del, open telemetry vil kunne være et alternativ. Slack tråd: https://nav-it.slack.com/archives/C06SJTR2X3L/p1724072054018589
            .header("Nav-Call-Id", correlationId.value)
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}

private fun mapStatus(
    status: Int,
    vedtak: Utbetalingsvedtak,
    request: String,
    response: String,
    token: AccessToken,
): Either<KunneIkkeUtbetale, SendtUtbetaling> {
    when (status) {
        202 -> {
            log.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "202 Accepted fra helved utsjekk for, utbetalingsvedtak ${vedtak.id}. Response: $response. Se sikkerlogg for mer kontekst."
            }
            sikkerlogg.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "202 Accepted fra helved utsjekk for, utbetalingsvedtak ${vedtak.id}. Response: $response. Request = $request"
            }
            return SendtUtbetaling(
                request = request,
                response = response,
                responseStatus = status,
            ).right()
        }

        400 -> {
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "400 Bad Request fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Denne vil bli prøvd på nytt. Response: $response. Se sikkerlogg for mer kontekst."
            }
            sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "400 Bad Request fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Denne vil bli prøvd på nytt. Response: $response. Request = $request"
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
                "$status fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Denne vil bli prøvd på nytt. Response: $response. Se sikkerlogg for mer kontekst."
            }
            sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "$status fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Denne vil bli prøvd på nytt. Response: $response. Request = $request"
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
                    "409 Conflict fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Vi antar vi har sendt samme melding tidligere og behandler denne på samme måte som 202 Response: $response. Se sikkerlogg for mer kontekst."
                }
                sikkerlogg.info(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Vi antar vi har sendt samme melding tidligere og behandler denne på samme måte som 202 Response: $response. Request = $request"
                }
                return SendtUtbetaling(
                    request = request,
                    response = response,
                    responseStatus = status,
                ).right()
            } else {
                log.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Vi forventet responsen 'Iverksettingen er allerede mottatt', men fikk $response. Se sikkerlogg for mer kontekst."
                }
                sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                    "409 Conflict fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Vi forventet responsen 'Iverksettingen er allerede mottatt', men fikk $response. Request = $request"
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
                "Ukjent feil fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Denne vil bli prøvd på nytt. Statuskode: $status, response: $response. Se sikkerlogg for mer kontekst."
            }
            sikkerlogg.error(RuntimeException("Trigger stacktrace for enklere debug.")) {
                "Ukjent feil fra helved utsjekk, for utbetalingsvedtak ${vedtak.id}. Denne vil bli prøvd på nytt. Statuskode: $status, response: $response. Request = $request"
            }
            return KunneIkkeUtbetale(
                request = request,
                response = response,
                responseStatus = status,
            ).left()
        }
    }
}
