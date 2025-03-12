package no.nav.tiltakspenger.saksbehandling.distribusjon.infra

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.distribusjon.DokdistGateway
import no.nav.tiltakspenger.saksbehandling.distribusjon.KunneIkkeDistribuereDokument
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DokdistHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 1.seconds,
) : DokdistGateway {

    private val log = KotlinLogging.logger {}

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val uri = URI.create("$baseUrl/rest/v1/distribuerjournalpost")

    override suspend fun distribuerDokument(
        journalpostId: JournalpostId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeDistribuereDokument, DistribusjonId> {
        return withContext(Dispatchers.IO) {
            val jsonPayload = journalpostId.toDokdistRequest()
            Either.catch {
                val token = getToken()
                val request = createRequest(jsonPayload, correlationId, token.token)
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val status = httpResponse.statusCode()
                val jsonResponse = httpResponse.body()
                if (status == 401 || status == 403) {
                    log.error(RuntimeException("Trigger stacktrace for debug.")) { "Invaliderer cache for systemtoken mot dokdist. status: $status." }
                    token.invaliderCache()
                }
                if (status != 409 && status != 200) {
                    log.error { "Feil ved kall til dokdist. journalpostId: $journalpostId. Status: $status. uri: $uri. Se sikkerlogg for detaljer." }
                    sikkerlogg.error { "Feil ved kall til dokdist. journalpostId: $journalpostId. Status: $status. uri: $uri. jsonPayload: $jsonPayload. jsonResponse: $jsonResponse" }
                    return@withContext KunneIkkeDistribuereDokument.left()
                }
                // 409 er en forventet statuskode ved forsøk på å distribuere samme dokument flere ganger.
                jsonResponse.dokdistResponseToDomain(log)
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(it) { "Feil ved kall til dokdist. journalpostId: $journalpostId. Se sikkerlogg for detaljer." }
                sikkerlogg.error(it) { "Feil ved kall til dokdist. journalpostId: $journalpostId. . jsonPayload: $jsonPayload, uri: $uri" }
                KunneIkkeDistribuereDokument
            }.flatten()
        }
    }

    private suspend fun createRequest(
        jsonPayload: String,
        correlationId: CorrelationId,
        token: String,
    ): HttpRequest? =
        HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .header("Nav-CallId", correlationId.value)
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
}
