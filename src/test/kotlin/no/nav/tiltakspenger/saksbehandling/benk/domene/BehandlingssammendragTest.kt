package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.mai
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class BehandlingssammendragTest {

    @Test
    fun `krever at kravtidspunkt finnes for søknadsbehandlinger`() {
        assertDoesNotThrow {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "0001"),
                startet = LocalDateTime.now(fixedClock),
                kravtidspunkt = LocalDateTime.now(fixedClock),
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
            )
        }

        assertThrows<IllegalArgumentException> {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "9001"),
                startet = LocalDateTime.now(fixedClock),
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
            )
        }
    }

    @Test
    fun `kravtidspunkt skal være null for ikke-søknadsbehandlinger`() {
        assertThrows<IllegalArgumentException> {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "9001"),
                startet = LocalDateTime.now(fixedClock),
                kravtidspunkt = LocalDateTime.now(fixedClock),
                behandlingstype = BehandlingssammendragType.REVURDERING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
            )
        }

        assertDoesNotThrow {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "9001"),
                startet = LocalDateTime.now(fixedClock),
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.REVURDERING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
            )
        }
    }
}
