package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
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
 * Kildekode: https://github.com/navikt/tiltakspenger-datadeling
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Klienten logger bevisst ikke selv: den returnerer [HttpKlientError] uendret, og den bærer all HTTP-kontekst (status, rå request/respons, throwable) via `metadata`.
 * Feilloggingen gjøres én gang av konsumenten ([no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService]), som i tillegg har domenekonteksten.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen (auth, statusregel, serialisering) kjører i test.
 */
class DatadelingHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : DatadelingClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            connectTimeout = connectTimeout,
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

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
    ): Either<HttpKlientError, Unit> =
        // Datadeling svarer alltid 200 ved suksess; alt annet skal være feil (paritet med gammel successStatus).
        httpKlient.postJsonUtenSvar(uri, SerialisertJson(jsonPayload), godta = Statusregel.Eksakt(200)).map { }
}
