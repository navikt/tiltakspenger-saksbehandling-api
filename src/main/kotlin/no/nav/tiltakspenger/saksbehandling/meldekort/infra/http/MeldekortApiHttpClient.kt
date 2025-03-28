package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiHttpClientGateway
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
                with("Feilrespons ved sending av ${meldeperiode.kjedeId}/${meldeperiode.id} til meldekort-api - status: $status") {
                    logger.error { this }
                    sikkerlogg.error { "$this - Response body: $body" }
                }
                return FeilVedSendingTilMeldekortApi.left()
            }
        }.mapLeft {
            with("Feil ved sending av ${meldeperiode.kjedeId} til meldekort-api") {
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

private fun Meldeperiode.tilBrukerDTO(): MeldeperiodeDTO {
    return MeldeperiodeDTO(
        id = this.id.toString(),
        kjedeId = this.kjedeId.toString(),
        versjon = this.versjon.value,
        fnr = this.fnr.verdi,
        saksnummer = this.saksnummer.toString(),
        sakId = this.sakId.toString(),
        opprettet = this.opprettet,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        antallDagerForPeriode = this.antallDagerForPeriode,
        girRett = this.girRett,
    )
}
