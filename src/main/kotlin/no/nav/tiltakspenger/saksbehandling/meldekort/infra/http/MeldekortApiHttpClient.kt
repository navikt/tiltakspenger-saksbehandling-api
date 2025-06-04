package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class MeldekortApiHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
) : MeldekortApiKlient {
    private val client = java.net.http.HttpClient
        .newBuilder()
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val logger = KotlinLogging.logger {}

    private val sakUrl = URI.create("$baseUrl/saksbehandling/sak")

    override suspend fun sendSak(sak: Sak): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Either.catch {
            val payload = serialize(sak.tilMeldekortApiDTO())

            val response = client.sendAsync(
                createRequest(sakUrl, payload),
                HttpResponse.BodyHandlers.ofString(),
            ).await()

            val status = response.statusCode()

            if (status !in 200..299) {
                val body: String = response.body()
                with("Feilrespons ved sending av sak ${sak.id} til meldekort-api - status: $status") {
                    logger.error { this }
                    Sikkerlogg.error { "$this - Response body: $body" }
                }
                return FeilVedSendingTilMeldekortApi.left()
            }
        }.mapLeft {
            with("Feil ved sending av sak ${sak.id} til meldekort-api") {
                logger.error { this }
                Sikkerlogg.error(it) { this }
            }
            FeilVedSendingTilMeldekortApi
        }
    }

    private suspend fun createRequest(
        url: URI,
        payload: String,
    ): HttpRequest {
        return HttpRequest
            .newBuilder()
            .uri(url)
            .header("Authorization", "Bearer ${getToken().token}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
    }
}

private fun Meldeperiode.tilMeldekortApiDTO(): SakTilMeldekortApiDTO.Meldeperiode {
    return SakTilMeldekortApiDTO.Meldeperiode(
        id = this.id.toString(),
        kjedeId = this.kjedeId.toString(),
        versjon = this.versjon.value,
        opprettet = this.opprettet,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        antallDagerForPeriode = this.maksAntallDagerForMeldeperiode,
        girRett = this.girRett,
    )
}

private fun Sak.tilMeldekortApiDTO(): SakTilMeldekortApiDTO {
    return SakTilMeldekortApiDTO(
        fnr = this.fnr.verdi,
        sakId = this.id.toString(),
        saksnummer = this.saksnummer.toString(),
        meldeperioder = this.meldeperiodeKjeder.sisteMeldeperiodePerKjede.map { it.tilMeldekortApiDTO() },
    )
}
