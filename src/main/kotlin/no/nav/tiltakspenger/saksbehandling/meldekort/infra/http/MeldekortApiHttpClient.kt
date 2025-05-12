package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiHttpClientGateway
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.LocalDate

class MeldekortApiHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    private val clock: Clock,
) : MeldekortApiHttpClientGateway {
    private val client = java.net.http.HttpClient
        .newBuilder()
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val logger = KotlinLogging.logger {}

    private val meldeperiodeUrl = URI.create("$baseUrl/saksbehandling/meldeperiode")
    private val sakUrl = URI.create("$baseUrl/saksbehandling/sak")

    override suspend fun sendMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Either.catch {
            val payload = serialize(meldeperiode.tilMeldekortApiDTO())

            val response = client.sendAsync(
                createRequest(meldeperiodeUrl, payload),
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

    override suspend fun sendSak(sak: Sak): Either<FeilVedSendingTilMeldekortApi, Unit> {
        return Either.catch {
            val payload = serialize(sak.tilMeldekortApiDTO(clock))

            val response = client.sendAsync(
                createRequest(sakUrl, payload),
                HttpResponse.BodyHandlers.ofString(),
            ).await()

            val status = response.statusCode()

            if (status !in 200..299) {
                val body: String = response.body()
                with("Feilrespons ved sending av sak ${sak.id} til meldekort-api - status: $status") {
                    logger.error { this }
                    sikkerlogg.error { "$this - Response body: $body" }
                }
                return FeilVedSendingTilMeldekortApi.left()
            }
        }.mapLeft {
            with("Feil ved sending av sak ${sak.id} til meldekort-api") {
                logger.error { this }
                sikkerlogg.error(it) { this }
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

private fun Meldeperiode.tilMeldekortApiDTO(): MeldeperiodeDTO {
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
        antallDagerForPeriode = this.maksAntallDagerForMeldeperiode,
        girRett = this.girRett,
    )
}

// TODO: flytt til libs
private data class SakDTO(
    val fnr: String,
    val sakId: String,
    val saksnummer: String,
    val meldeperioder: List<PeriodeDTO>,
)

private fun Sak.tilMeldekortApiDTO(clock: Clock): SakDTO {
    // TODO: hvis/når vi forhåndsgenererer alle meldeperioder for hvert vedtak, så kan vi hente meldeperiodene fra saken
    val alleMeldeperioder = if (this.vedtaksliste.isNotEmpty()) {
        MeldeperiodeKjeder(emptyList())
            .genererMeldeperioder(
                vedtaksliste = this.vedtaksliste,
                ikkeGenererEtter = LocalDate.MAX,
                clock = clock,
            ).second.map { it.periode.toDTO() }
    } else {
        emptyList()
    }

    return SakDTO(
        fnr = this.fnr.verdi,
        sakId = this.id.toString(),
        saksnummer = this.saksnummer.toString(),
        meldeperioder = alleMeldeperioder,
    )
}
