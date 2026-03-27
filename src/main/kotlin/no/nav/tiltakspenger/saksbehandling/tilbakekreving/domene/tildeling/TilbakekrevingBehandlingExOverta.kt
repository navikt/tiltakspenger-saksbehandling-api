package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import java.time.Clock

/** Saksbehandler/beslutter overtar tilbakekrevingsbehandlingen fra en annen. */
fun TilbakekrevingBehandling.overta(saksbehandler: Saksbehandler, clock: Clock): TilbakekrevingBehandling {
    val sistEndret = nå(clock)

    return when (statusIntern) {
        TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING -> {
            requireNotNull(this.saksbehandler) {
                "Saksbehandler må være satt på behandlingen for å kunne overta. tilbakekrevingId: $id"
            }
            require(this.saksbehandler != saksbehandler.navIdent) {
                "Kan ikke overta fra seg selv. tilbakekrevingId: $id"
            }

            krevSaksbehandlerRolle(saksbehandler)

            this.copy(
                saksbehandler = saksbehandler.navIdent,
                beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING -> {
            requireNotNull(this.beslutter) {
                "Beslutter må være satt på behandlingen for å kunne overta. tilbakekrevingId: $id"
            }
            require(this.beslutter != saksbehandler.navIdent) {
                "Kan ikke overta fra seg selv. tilbakekrevingId: $id"
            }
            check(saksbehandler.navIdent != this.saksbehandler) {
                "Beslutter (${saksbehandler.navIdent}) kan ikke være den samme som saksbehandleren (${this.saksbehandler})"
            }

            krevBeslutterRolle(saksbehandler)

            this.copy(
                beslutter = saksbehandler.navIdent,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatusIntern.OPPRETTET,
        TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING,
        TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING,
        TilbakekrevingBehandlingsstatusIntern.AVSLUTTET,
        -> throw IllegalArgumentException(
            "Kan ikke overta behandling med status $status. Bruk ta() for å ta en ledig behandling. tilbakekrevingId: $id",
        )
    }
}
