package no.nav.tiltakspenger.saksbehandling.benk.domene

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.mai
import org.junit.jupiter.api.Test

class BehandlingssammendragTest {

    @Test
    fun `krever at kravtidspunkt finnes for søknadsbehandlinger`() {
        shouldNotThrowAny {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "0001"),
                startet = nå(fixedClock),
                kravtidspunkt = nå(fixedClock),
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = null,
                erUnderkjent = false,
                resultat = null,
                beløp = null,
            )
        }

        shouldThrow<IllegalArgumentException> {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "9001"),
                startet = nå(fixedClock),
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = null,
                erUnderkjent = false,
                resultat = null,
                beløp = null,
            )
        }
    }

    @Test
    fun `kravtidspunkt skal være null for ikke-søknadsbehandlinger`() {
        shouldThrow<IllegalArgumentException> {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "9001"),
                startet = nå(fixedClock),
                kravtidspunkt = nå(fixedClock),
                behandlingstype = BehandlingssammendragType.REVURDERING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = null,
                erUnderkjent = false,
                resultat = null,
                beløp = null,
            )
        }

        shouldNotThrowAny {
            Behandlingssammendrag(
                sakId = SakId.random(),
                fnr = Fnr.random(),
                saksnummer = Saksnummer.genererSaknummer(1.mai(2025), "9001"),
                startet = nå(fixedClock),
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.REVURDERING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = null,
                erUnderkjent = false,
                resultat = null,
                beløp = null,
            )
        }
    }
}
