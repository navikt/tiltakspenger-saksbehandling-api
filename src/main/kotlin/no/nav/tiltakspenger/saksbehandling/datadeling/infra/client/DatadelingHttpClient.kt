package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.datadeling.DatadelingClient
import no.nav.tiltakspenger.saksbehandling.datadeling.FeilVedSendingTilDatadeling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DatadelingHttpClient(
    baseUrl: String,
    val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 1.seconds,
) : DatadelingClient {
    private val log = KotlinLogging.logger {}

    private val client = HttpClient
        .newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private val behandlingsUri = URI.create("$baseUrl/behandling")
    private val vedtaksUri = URI.create("$baseUrl/vedtak")
    private val meldeperioderUri = URI.create("$baseUrl/meldeperioder")
    private val meldekortUri = URI.create("$baseUrl/meldekort")

    override suspend fun send(
        rammevedtak: Rammevedtak,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit> {
        val jsonPayload = rammevedtak.toDatadelingJson()
        return Either.catch {
            val request = createRequest(jsonPayload, vedtaksUri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                log.error { "Feil ved kall til tiltakspenger-datadeling. Vedtak ${rammevedtak.id}, saksnummer ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}. Status: $status. uri: $vedtaksUri. Se sikkerlogg for detaljer." }
                Sikkerlogg.error { "Feil ved kall til tiltakspenger-datadeling. Vedtak ${rammevedtak.id}, saksnummer ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}. uri: $vedtaksUri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                return FeilVedSendingTilDatadeling.left()
            }
            Unit
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            log.error(it) { "Feil ved kall til tiltakspenger-datadeling. Vedtak ${rammevedtak.id}, saksnummer ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}. uri: $vedtaksUri. Se sikkerlogg for detaljer." }
            Sikkerlogg.error(it) { "Feil ved kall til tiltakspenger-datadeling. Vedtak ${rammevedtak.id}, saksnummer ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}, uri: $vedtaksUri, jsonPayload: $jsonPayload" }
            FeilVedSendingTilDatadeling
        }
    }

    override suspend fun send(
        behandling: Behandling,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit> {
        val jsonPayload = if (behandling is Rammebehandling) {
            behandling.toBehandlingJson()
        } else if (behandling is MeldekortBehandling) {
            behandling.toBehandlingJson()
        } else {
            throw IllegalStateException("Kan ikke dele behandling med id ${behandling.id} som ikke er rammebehandling eller meldekortbehandling")
        }
        return Either.catch {
            val request = createRequest(jsonPayload, behandlingsUri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                log.error { "Feil ved kall til tiltakspenger-datadeling. Behandling ${behandling.id}, saksnummer ${behandling.saksnummer}, sakId: ${behandling.sakId}. Status: $status. uri: $behandlingsUri. Se sikkerlogg for detaljer." }
                Sikkerlogg.error { "Feil ved kall til tiltakspenger-datadeling. Behandling ${behandling.id}, saksnummer ${behandling.saksnummer}, sakId: ${behandling.sakId}. uri: $behandlingsUri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                return FeilVedSendingTilDatadeling.left()
            }
            Unit
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            log.error(it) { "Feil ved kall til datadeling. Vedtak ${behandling.id}, saksnummer ${behandling.saksnummer}, sakId: ${behandling.sakId}. Se sikkerlogg for detaljer." }
            Sikkerlogg.error(it) { "Feil ved kall til datadeling. Vedtak ${behandling.id}, saksnummer ${behandling.saksnummer}, sakId: ${behandling.sakId}. jsonPayload: $jsonPayload, uri: $behandlingsUri" }
            FeilVedSendingTilDatadeling
        }
    }

    override suspend fun send(
        sak: Sak,
        meldeperioder: List<Meldeperiode>,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit> {
        require(meldeperioder.none { it.sakId != sak.id }) {
            "Alle meldeperiodene må tilhøre innsendt sak, sakId ${sak.id}, correlationId $correlationId"
        }
        val jsonPayload = meldeperioder.toDatadelingJson(sak)
        return Either.catch {
            val request = createRequest(jsonPayload, meldeperioderUri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                log.error { "Feil ved kall til tiltakspenger-datadeling. Meldeperioder for saksnummer ${sak.saksnummer}, sakId: ${sak.id}. Status: $status. uri: $meldeperioderUri. Se sikkerlogg for detaljer." }
                Sikkerlogg.error { "Feil ved kall til tiltakspenger-datadeling. Meldeperioder for saksnummer ${sak.saksnummer}, sakId: ${sak.id}. uri: $meldeperioderUri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                return FeilVedSendingTilDatadeling.left()
            }
            Unit
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            log.error(it) { "Feil ved kall til tiltakspenger-datadeling. Meldeperioder for saksnummer ${sak.saksnummer}, sakId: ${sak.id}. uri: $meldeperioderUri. Se sikkerlogg for detaljer." }
            Sikkerlogg.error(it) { "Feil ved kall til tiltakspenger-datadeling. Meldeperioder for saksnummer ${sak.saksnummer}, sakId: ${sak.id}, uri: $meldeperioderUri, jsonPayload: $jsonPayload" }
            FeilVedSendingTilDatadeling
        }
    }

    override suspend fun send(
        godkjentMeldekort: MeldekortBehandling.Behandlet,
        clock: Clock,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit> {
        require(godkjentMeldekort.erGodkjent) {
            "Meldekortet er ikke godkjent, meldekortId ${godkjentMeldekort.id}, correlationId $correlationId"
        }
        val jsonPayload = godkjentMeldekort.toDatadelingJson(clock)
        return Either.catch {
            val request = createRequest(jsonPayload, meldekortUri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                log.error { "Feil ved kall til tiltakspenger-datadeling. Meldekort med id ${godkjentMeldekort.id} for saksnummer ${godkjentMeldekort.saksnummer}, sakId: ${godkjentMeldekort.sakId}. Status: $status. uri: $meldekortUri. Se sikkerlogg for detaljer." }
                Sikkerlogg.error { "Feil ved kall til tiltakspenger-datadeling. Meldekort med id ${godkjentMeldekort.id} for saksnummer ${godkjentMeldekort.saksnummer}, sakId: ${godkjentMeldekort.sakId}. uri: $meldekortUri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                return FeilVedSendingTilDatadeling.left()
            }
            Unit
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            log.error(it) { "Feil ved kall til tiltakspenger-datadeling. Meldekort med id ${godkjentMeldekort.id} for saksnummer ${godkjentMeldekort.saksnummer}, sakId: ${godkjentMeldekort.sakId}. uri: $meldekortUri. Se sikkerlogg for detaljer." }
            Sikkerlogg.error(it) { "Feil ved kall til tiltakspenger-datadeling. Meldekort med id ${godkjentMeldekort.id} for saksnummer ${godkjentMeldekort.saksnummer}, sakId: ${godkjentMeldekort.sakId}, uri: $meldekortUri, jsonPayload: $jsonPayload" }
            FeilVedSendingTilDatadeling
        }
    }

    private suspend fun createRequest(
        jsonPayload: String,
        uri: URI,
    ): HttpRequest? =
        HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer ${getToken().token}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
}
