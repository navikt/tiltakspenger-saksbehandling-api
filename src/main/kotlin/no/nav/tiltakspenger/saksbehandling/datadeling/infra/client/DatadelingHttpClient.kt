package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.datadeling.DatadelingClient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakDb
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tiltakspenger-datadeling, bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Klienten logger bevisst ikke selv: den returnerer [HttpKlientError] uendret, og den bærer all HTTP-kontekst (status, rå request/respons, throwable) via `metadata`.
 * Feilloggingen gjøres én gang av konsumenten ([no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService]), som i tillegg har domenekonteksten.
 *
 * [httpKlient] bygges som default ut fra parametrene over ([clock], [authTokenProvider], [connectTimeout], [defaultTimeout], [successStatus]) slik at hele klientoppsettet kan leses ett sted.
 * Sender man inn en egen [httpKlient] (typisk `HttpKlientFake` i test), **ignoreres** de parametrene som kun brukes til å bygge default-klienten.
 *
 * @param clock Klokke som sendes videre til [HttpKlient]. Ignoreres hvis [httpKlient] sendes inn.
 * @param authTokenProvider Henter system-token mot datadeling. Ignoreres hvis [httpKlient] sendes inn.
 * @param connectTimeout Connect-timeout for default-klienten. Ignoreres hvis [httpKlient] sendes inn.
 * @param defaultTimeout Per-request timeout for default-klienten. Ignoreres hvis [httpKlient] sendes inn.
 * @param successStatus Predikat for hvilke HTTP-statuser som regnes som suksess i default-klienten. Ignoreres hvis [httpKlient] sendes inn.
 */
class DatadelingHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    defaultTimeout: Duration = 10.seconds,
    successStatus: (Int) -> Boolean = { it == 200 },
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = defaultTimeout
        this.successStatus = successStatus
        this.authTokenProvider = authTokenProvider
    },
) : DatadelingClient {
    private val behandlingsUri = URI.create("$baseUrl/behandling")
    private val vedtaksUri = URI.create("$baseUrl/vedtak")
    private val meldeperioderUri = URI.create("$baseUrl/meldeperioder")
    private val meldekortUri = URI.create("$baseUrl/meldekort")
    private val sakUri = URI.create("$baseUrl/sak")

    override suspend fun send(
        rammevedtak: Rammevedtak,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> = sendTilDatadeling(rammevedtak.toDatadelingJson(), vedtaksUri)

    override suspend fun send(
        behandling: AttesterbarBehandling,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        val jsonPayload = when (behandling) {
            is Rammebehandling -> behandling.toBehandlingJson()
            is Meldekortbehandling -> behandling.toBehandlingJson()
            else -> throw IllegalStateException("Kan ikke dele behandling med id ${behandling.id} som ikke er rammebehandling eller meldekortbehandling")
        }
        return sendTilDatadeling(jsonPayload, behandlingsUri)
    }

    override suspend fun send(
        meldeperioder: List<Meldeperiode>,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        val sakId = meldeperioder.firstOrNull()?.sakId ?: throw IllegalStateException("Kan ikke dele tom liste med meldeperioder")
        return sendTilDatadeling(meldeperioder.toDatadelingJson(sakId), meldeperioderUri)
    }

    override suspend fun send(
        meldekortvedtak: Meldekortvedtak,
        differansePerKjede: Map<MeldeperiodeKjedeId, Int>?,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        require(meldekortvedtak.journalpostId != null) {
            "Kan ikke dele meldekortvedtak med id ${meldekortvedtak.id} som ikke er journalført ennå"
        }
        return sendTilDatadeling(meldekortvedtak.toDatadelingJson(differansePerKjede), meldekortUri)
    }

    override suspend fun send(
        sakDb: SakDb,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> = sendTilDatadeling(sakDb.toDatadelingJson(), sakUri)

    private suspend fun sendTilDatadeling(
        jsonPayload: String,
        uri: URI,
    ): Either<HttpKlientError, Unit> = httpKlient.post<Unit>(uri, jsonPayload).map { }
}
