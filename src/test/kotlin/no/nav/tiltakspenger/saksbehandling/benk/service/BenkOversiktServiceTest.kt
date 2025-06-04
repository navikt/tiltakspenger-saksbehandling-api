package no.nav.tiltakspenger.saksbehandling.benk.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BenkOversiktServiceTest {

    @Test
    fun `filtrerer vekk behandlinger saksbehandler ikke har tilgang til`() {
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
        )
        val bs2HarIkkeTilgang = Behandlingssammendrag(
            sakId = SakId.random(),
            fnr = Fnr.random(),
            saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "9001"),
            startet = LocalDateTime.now(fixedClock),
            kravtidspunkt = LocalDateTime.now(fixedClock),
            behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
            status = BehandlingssammendragStatus.UNDER_BEHANDLING,
            saksbehandler = ObjectMother.saksbehandler().navIdent,
            beslutter = null,
        )

        val tilganger = mapOf(bs1HarTilgang.fnr to true, bs2HarIkkeTilgang.fnr to false)
        val oversikt = BenkOversikt(behandlingssammendrag = listOf(bs1HarTilgang, bs2HarIkkeTilgang), totalAntall = 2)

        val actual =
            filtrerIkkeTilgang(oversikt, tilganger, ObjectMother.saksbehandler(), logger = KotlinLogging.logger {})

        actual.behandlingssammendrag shouldBe listOf(bs1HarTilgang)
        actual.totalAntall shouldBe 2
    }
}
