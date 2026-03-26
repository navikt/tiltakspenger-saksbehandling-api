package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import java.time.Clock

/** Saksbehandler tar tilbakekrevingsbehandlingen. */
fun TilbakekrevingBehandling.taBehandling(saksbehandler: Saksbehandler, clock: Clock): TilbakekrevingBehandling {
    val sistEndret = nå(clock)

    return when (statusIntern) {
        TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING -> {
            require(this.saksbehandlerIdent == null) {
                "Saksbehandler skal ikke kunne være satt på behandlingen dersom den er TIL_BEHANDLING"
            }

            this.copy(
                saksbehandlerIdent = saksbehandler.navIdent,
                beslutterIdent = if (saksbehandler.navIdent == beslutterIdent) null else beslutterIdent,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING -> {
            check(saksbehandler.navIdent != this.saksbehandlerIdent) {
                "Beslutter (${saksbehandler.navIdent}) kan ikke være den samme som saksbehandleren (${this.saksbehandlerIdent})"
            }
            require(this.beslutterIdent == null) {
                "Behandlingen har en eksisterende beslutter. For å overta behandlingen, bruk overta()"
            }

            this.copy(
                beslutterIdent = saksbehandler.navIdent,
                sistEndret = sistEndret,
            )
        }

        TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING,
        TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING,
        -> throw IllegalStateException(
            "Kan ikke ta behandling som allerede er tatt. For å overta, bruk overta(). tilbakekrevingId: $id, status: $status",
        )

        TilbakekrevingBehandlingsstatusIntern.OPPRETTET,
        TilbakekrevingBehandlingsstatusIntern.AVSLUTTET,
        -> throw IllegalArgumentException(
            "Kan ikke ta behandling når behandlingen har status $status. tilbakekrevingId: $id",
        )
    }
}
