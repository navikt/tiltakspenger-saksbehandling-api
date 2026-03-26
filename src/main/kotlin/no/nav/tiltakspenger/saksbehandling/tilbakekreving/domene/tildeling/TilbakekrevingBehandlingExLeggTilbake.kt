package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import java.time.Clock

/** Saksbehandler/beslutter legger tilbake tilbakekrevingsbehandlingen. */
fun TilbakekrevingBehandling.leggTilbake(saksbehandler: Saksbehandler, clock: Clock): TilbakekrevingBehandling {
    val sistEndret = nå(clock)

    return when (status) {
        TilbakekrevingBehandlingsstatus.UNDER_BEHANDLING -> {
            require(this.saksbehandlerIdent == saksbehandler.navIdent) {
                "Kan bare legge tilbake behandling dersom saksbehandler selv er på behandlingen. tilbakekrevingId: $id"
            }

            this.copy(
                saksbehandlerIdent = null,
                status = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatus.UNDER_GODKJENNING -> {
            require(this.beslutterIdent == saksbehandler.navIdent) {
                "Kan bare legge tilbake behandling dersom beslutter selv er på behandlingen. tilbakekrevingId: $id"
            }

            this.copy(
                beslutterIdent = null,
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatus.OPPRETTET,
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
        TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
        TilbakekrevingBehandlingsstatus.AVSLUTTET,
        -> throw IllegalArgumentException(
            "Kan ikke legge tilbake behandling med status $status. tilbakekrevingId: $id",
        )
    }
}
