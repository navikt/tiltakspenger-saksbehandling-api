package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteOppfølgingsenhet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Klientkall
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.VeilarboppfolgingKlient
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class VeilarboppfolgingHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: kotlin.time.Duration = 1.seconds,
    private val timeout: kotlin.time.Duration = 1.seconds,
) : VeilarboppfolgingKlient {
    private val logger = KotlinLogging.logger {}
    private val client =
        java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build()

    private val uri = URI.create("$baseUrl/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus")

    override suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String?,
        saksnummer: String?,
        rammebehandlingId: String?,
        meldekortbehandlingId: String?,
    ): Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata> {
        val kontekst = lagKontekst(sakId, saksnummer, rammebehandlingId, meldekortbehandlingId)
        return withContext(Dispatchers.IO) {
            val jsonPayload = objectMapper.writeValueAsString(Request(fnr.verdi))
            Either.catch {
                val request = createRequest(jsonPayload, getToken().token)
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val status = httpResponse.statusCode()
                val body: String? = httpResponse.body()
                Pair(status, body)
            }.fold(
                ifLeft = {
                    // Either.catch slipper igjennom CancellationException som er ønskelig.
                    // Vi logger throwable kun til sikkerlogg fordi den kan inneholde utdrag av request/respons (med persondata).
                    logger.error(RuntimeException("Trigger stacktrace for enklere debug")) {
                        "Ukjent feil ved kall til veilarboppfolging. $kontekst. uri: $uri. Se sikkerlogg for detaljer."
                    }
                    Sikkerlogg.error(it) { "Feil ved kall til veilarboppfolging. $kontekst. uri: $uri. Request: $jsonPayload." }
                    KanIkkeHenteOppfølgingsenhet.KallFeilet(
                        veilarboppfolgingKall = Klientkall(request = jsonPayload, response = null, httpStatus = null),
                    ).left()
                },
                ifRight = { (status, body) ->
                    val kall = Klientkall(request = jsonPayload, response = body, httpStatus = status)
                    if (status != 200) {
                        logger.error { "Kunne ikke hente oppfølgingsenhet fra veilarboppfølging. $kontekst. Statuskode $status. Se sikkerlogg for detaljer." }
                        Sikkerlogg.error { "Kunne ikke hente oppfølgingsenhet fra veilarboppfølging. $kontekst. Statuskode $status. Request: $jsonPayload. Response: $body." }
                        return@fold KanIkkeHenteOppfølgingsenhet.UventetHttpStatus(status = status, veilarboppfolgingKall = kall).left()
                    }
                    Either.catch { objectMapper.readValue<Response>(body!!) }.fold(
                        ifLeft = { parseFeil ->
                            logger.error(RuntimeException("Trigger stacktrace for enklere debug")) {
                                "Klarte ikke å deserialisere respons fra veilarboppfølging. $kontekst. Se sikkerlogg for detaljer."
                            }
                            Sikkerlogg.error(parseFeil) { "Klarte ikke å deserialisere respons fra veilarboppfølging. $kontekst. Request: $jsonPayload. Response: $body." }
                            KanIkkeHenteOppfølgingsenhet.KallFeilet(veilarboppfolgingKall = kall).left()
                        },
                        ifRight = { parsed ->
                            val oppfolgingsenhet = parsed.oppfolgingsenhet
                            if (oppfolgingsenhet == null) {
                                logger.error { "Fant ikke oppfølgingsenhet. $kontekst. Se sikkerlogg for detaljer." }
                                Sikkerlogg.error { "Fant ikke oppfølgingsenhet for fnr ${fnr.verdi} - $kontekst. Request: $jsonPayload. Response: $body" }
                                KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet(veilarboppfolgingKall = kall).left()
                            } else {
                                NavkontorMedMetadata(
                                    navkontor = oppfolgingsenhet.toNavkontor(),
                                    brukteKlient = BruktNavkontorKlient.VEILARBOPPFOLGING,
                                    veilarboppfolgingKall = kall,
                                ).right()
                            }
                        },
                    )
                },
            )
        }
    }

    private fun createRequest(
        jsonPayload: String,
        token: String,
    ): HttpRequest? {
        return HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Nav-Consumer-Id", "tiltakspenger-saksbehandling-api")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}

private fun lagKontekst(
    sakId: String?,
    saksnummer: String?,
    rammebehandlingId: String?,
    meldekortbehandlingId: String?,
): String =
    listOfNotNull(
        sakId?.let { "sakId=$it" },
        saksnummer?.let { "saksnummer=$it" },
        rammebehandlingId?.let { "rammebehandlingId=$it" },
        meldekortbehandlingId?.let { "meldekortbehandlingId=$it" },
    ).ifEmpty { listOf("ingen kontekst") }.joinToString(", ")

private data class Request(
    val fnr: String,
)

private data class Response(
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val veilederId: String?,
    val formidlingsgruppe: String?,
    val servicegruppe: String?,
    val hovedmaalkode: String?,
)

private data class Oppfolgingsenhet(
    val navn: String,
    val enhetId: String,
) {
    fun toNavkontor(): Navkontor =
        Navkontor(
            kontornummer = enhetId,
            kontornavn = navn,
        )
}
