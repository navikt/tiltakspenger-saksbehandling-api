package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import java.time.Clock

/** Saksbehandler/beslutter overtar tilbakekrevingsbehandlingen fra en annen. */
fun TilbakekrevingBehandling.overta(saksbehandler: Saksbehandler, clock: Clock): TilbakekrevingBehandling {
    val sistEndret = nå(clock)

    return when (status) {
        TilbakekrevingBehandlingsstatus.UNDER_BEHANDLING -> {
            requireNotNull(this.saksbehandlerIdent) {
                "Saksbehandler må være satt på behandlingen for å kunne overta. tilbakekrevingId: $id"
            }
            require(this.saksbehandlerIdent != saksbehandler.navIdent) {
                "Kan ikke overta fra seg selv. tilbakekrevingId: $id"
            }

            this.copy(
                saksbehandlerIdent = saksbehandler.navIdent,
                beslutterIdent = if (saksbehandler.navIdent == beslutterIdent) null else beslutterIdent,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatus.UNDER_GODKJENNING -> {
            requireNotNull(this.beslutterIdent) {
                "Beslutter må være satt på behandlingen for å kunne overta. tilbakekrevingId: $id"
            }
            require(this.beslutterIdent != saksbehandler.navIdent) {
                "Kan ikke overta fra seg selv. tilbakekrevingId: $id"
            }
            check(saksbehandler.navIdent != this.saksbehandlerIdent) {
                "Beslutter (${saksbehandler.navIdent}) kan ikke være den samme som saksbehandleren (${this.saksbehandlerIdent})"
            }

            this.copy(
                beslutterIdent = saksbehandler.navIdent,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatus.OPPRETTET,
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
        TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
        TilbakekrevingBehandlingsstatus.AVSLUTTET,
        -> throw IllegalArgumentException(
            "Kan ikke overta behandling med status $status. Bruk ta() for å ta en ledig behandling. tilbakekrevingId: $id",
        )
    }
}
