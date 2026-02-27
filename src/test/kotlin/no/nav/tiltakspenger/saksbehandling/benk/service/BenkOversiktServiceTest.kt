package no.nav.tiltakspenger.saksbehandling.benk.service

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.SorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.domene.ÅpneBehandlingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BenkOversiktServiceTest {

    @Test
    fun `filtrerer vekk behandlinger saksbehandler ikke har tilgang til`() {
        runTest {
            val saksbehandler = ObjectMother.saksbehandler()
            val benkoversiktMock = mockk<BenkOversiktRepo>()
            val tilgangsserviceMock = mockk<TilgangskontrollService>()

            val service =
                BenkOversiktService(benkOversiktRepo = benkoversiktMock, tilgangskontrollService = tilgangsserviceMock)

            val bs1HarTilgang = Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "9000"),
                startet = LocalDateTime.now(fixedClock),
                kravtidspunkt = LocalDateTime.now(fixedClock),
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                saksbehandler = "saksbehandler 1",
                beslutter = null,
                erSattPåVent = false,
                sistEndret = null,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                resultat = null,
                erUnderkjent = false,
            )
            val bs2HarIkkeTilgang = Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "9001"),
                startet = LocalDateTime.now(fixedClock),
                kravtidspunkt = LocalDateTime.now(fixedClock),
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                saksbehandler = saksbehandler.navIdent,
                beslutter = null,
                erSattPåVent = false,
                sistEndret = null,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                resultat = null,
                erUnderkjent = false,
            )

            val tilganger = mapOf(bs1HarTilgang.fnr to true, bs2HarIkkeTilgang.fnr to false)

            coEvery { benkoversiktMock.hentÅpneBehandlinger(any()) } returns BenkOversikt(
                behandlingssammendrag = listOf(
                    bs1HarTilgang,
                    bs2HarIkkeTilgang,
                ),
                totalAntall = 2,
            )

            coEvery {
                tilgangsserviceMock.harTilgangTilPersoner(
                    fnrs = any(),
                    saksbehandlerToken = any(),
                    saksbehandler = any(),
                )
            } returns tilganger

            val actual = service.hentBenkOversikt(
                command = HentÅpneBehandlingerCommand(
                    åpneBehandlingerFiltrering = ÅpneBehandlingerFiltrering(null, null, null, null),
                    sortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.ASC),
                    saksbehandler = saksbehandler,
                    correlationId = CorrelationId.generate(),
                ),
                saksbehandlerToken = "secret tokenz",
                saksbehandler = saksbehandler,

            )

            actual.behandlingssammendrag shouldBe listOf(bs1HarTilgang)
            actual.totalAntall shouldBe 2
            actual.antallFiltrertPgaTilgang shouldBe 1
        }
    }
}
