package no.nav.tiltakspenger.vedtak.clients.meldekort

import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.meldekort.ports.MeldekortApiHttpClientGateway
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class MeldekortApiHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
) : MeldekortApiHttpClientGateway {
    private val client = java.net.http.HttpClient
        .newBuilder()
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val logger = KotlinLogging.logger {}

    private val meldekortApiUri = URI.create("$baseUrl/meldekort")

    override suspend fun sendMeldekort(meldekort: Meldekort): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Either.catch {
            val response = client.sendAsync(
                createRequest(meldekort),
                HttpResponse.BodyHandlers.ofString(),
            ).await()

            val status = response.statusCode()

            if (status !in 200..299) {
                val body: String = response.body()
                "Feilrespons ved sending av ${meldekort.id} til meldekort-api - status: $status".let { msg ->
                    logger.error(msg)
                    sikkerlogg.error { "$msg - Response body: $body" }
                }
                return FeilVedSendingTilMeldekortApi.left()
            }
        }.mapLeft {
            "Feil ved sending av ${meldekort.id} til meldekort-api".let { msg ->
                logger.error { msg }
                sikkerlogg.error(it) { msg }
            }
            FeilVedSendingTilMeldekortApi
        }
    }

    private suspend fun createRequest(
        meldekort: Meldekort,
    ): HttpRequest {
        val payload = serialize(meldekort.tilBrukerDTO())

        return HttpRequest
            .newBuilder()
            .uri(meldekortApiUri)
            .header("Authorization", "Bearer ${getToken().token}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
    }
}
