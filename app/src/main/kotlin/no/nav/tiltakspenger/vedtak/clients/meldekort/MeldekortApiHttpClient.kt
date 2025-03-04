package no.nav.tiltakspenger.vedtak.clients.meldekort

import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
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

    override suspend fun sendMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Either.catch {
            val response = client.sendAsync(
                createRequest(meldeperiode),
                HttpResponse.BodyHandlers.ofString(),
            ).await()

            val status = response.statusCode()

            if (status !in 200..299) {
                val body: String = response.body()
                with("Feilrespons ved sending av ${meldeperiode.meldeperiodeKjedeId}/${meldeperiode.id} til meldekort-api - status: $status") {
                    logger.error(this)
                    sikkerlogg.error { "$this - Response body: $body" }
                }
                return FeilVedSendingTilMeldekortApi.left()
            }
        }.mapLeft {
            with("Feil ved sending av ${meldeperiode.meldeperiodeKjedeId} til meldekort-api") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            FeilVedSendingTilMeldekortApi
        }
    }

    private suspend fun createRequest(
        meldeperiode: Meldeperiode,
    ): HttpRequest {
        val payload = serialize(meldeperiode.tilBrukerDTO())

        return HttpRequest
            .newBuilder()
            .uri(meldekortApiUri)
            .header("Authorization", "Bearer ${getToken().token}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
    }
}
