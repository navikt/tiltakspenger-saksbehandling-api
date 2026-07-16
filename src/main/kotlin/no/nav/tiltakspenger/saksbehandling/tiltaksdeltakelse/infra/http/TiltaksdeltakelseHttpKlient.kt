package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tiltakspenger-tiltak for å hente tiltaksdeltakelser, bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-tiltak
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Klienten logger bevisst ikke selv: den returnerer [HttpKlientError] uendret, og feillogging gjøres én gang i kallende service ([no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService], [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service.TiltaksdeltakelseService]), som i tillegg har domenekonteksten.
 *
 * @param timeout Per-request timeout. tiltakspenger-tiltak gjør oppslag mot flere kilder, derav den høye defaulten (matcher den gamle ktor-klientens 60s).
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class TiltaksdeltakelseHttpKlient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 60.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : TiltaksdeltakelseKlient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            connectTimeout = connectTimeout,
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    companion object {
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
    }

    private val tiltakshistorikkUri = URI.create("$baseUrl/azure/tiltakshistorikk")

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, TiltaksdeltakelserFraRegister> {
        return hentTiltakshistorikk(fnr, tiltaksdeltakelserDetErSøktTiltakspengerFor, correlationId)
            .map { mapTiltak(it) }
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<TiltaksdeltakelseMedArrangørnavn>> {
        return hentTiltakshistorikk(fnr, TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(), correlationId)
            .map {
                mapTiltakMedArrangørnavn(
                    harAdressebeskyttelse = harAdressebeskyttelse,
                    tiltakDTOListe = it,
                )
            }
    }

    private suspend fun hentTiltakshistorikk(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<TiltakshistorikkDTO>> {
        // tiltakspenger-tiltak svarer alltid 200 ved suksess; alt annet skal være feil (paritet med gammel successStatus).
        return httpKlient.postJson<List<TiltakshistorikkDTO>>(
            uri = tiltakshistorikkUri,
            body = TiltakRequestDTO(fnr.verdi),
            headere = listOf(NavHeadere.navCallId(correlationId.value)),
            godta = Statusregel.Eksakt(200),
        ).map { response ->
            response.body
                .filter { it.harFomOgTomEllerRelevantStatus(tiltaksdeltakelserDetErSøktTiltakspengerFor) }
                .filter { it.rettPaTiltakspenger() }
        }
    }
}
