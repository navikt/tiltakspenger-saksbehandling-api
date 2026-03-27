package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import java.time.Clock

/** Saksbehandler/beslutter legger tilbake tilbakekrevingsbehandlingen. */
fun TilbakekrevingBehandling.leggTilbake(saksbehandler: Saksbehandler, clock: Clock): TilbakekrevingBehandling {
    val sistEndret = nå(clock)

    return when (statusIntern) {
        TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING -> {
            require(this.saksbehandler == saksbehandler.navIdent) {
                "Kan bare legge tilbake behandling dersom saksbehandler selv er på behandlingen. tilbakekrevingId: $id"
            }

            this.copy(
                saksbehandler = null,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING -> {
            require(this.beslutter == saksbehandler.navIdent) {
                "Kan bare legge tilbake behandling dersom beslutter selv er på behandlingen. tilbakekrevingId: $id"
            }

            this.copy(
                beslutter = null,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatusIntern.OPPRETTET,
        TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING,
        TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING,
        TilbakekrevingBehandlingsstatusIntern.AVSLUTTET,
        -> throw IllegalArgumentException(
            "Kan ikke legge tilbake behandling med status $status. tilbakekrevingId: $id",
        )
    }
}
