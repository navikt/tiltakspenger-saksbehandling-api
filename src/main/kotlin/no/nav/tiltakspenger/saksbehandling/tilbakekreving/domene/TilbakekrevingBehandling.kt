package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 *  [tilbakeBehandlingId] BehandlingId i ekstern saksbehandlingsløsning for tilbakekreving
 *  [url] URL til behandlingen i ekstern saksbehandlingsløsning for tilbakekreving
 * */
data class TilbakekrevingBehandling(
    val id: TilbakekrevingId,
    val sakId: SakId,
    val utbetalingId: UtbetalingId,
    val tilbakeBehandlingId: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val status: TilbakekrevingBehandlingsstatus,
    val url: String,
    val kravgrunnlagTotalPeriode: Periode,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val varselSendt: LocalDate?,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
) {

    /** Saksbehandler tar tilbakekrevingsbehandlingen. */
    fun taBehandling(saksbehandler: Saksbehandler, nå: LocalDateTime): TilbakekrevingBehandling {
        return when (status) {
            TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> {
                require(this.saksbehandlerIdent == null) {
                    "Saksbehandler skal ikke kunne være satt på behandlingen dersom den er TIL_BEHANDLING"
                }
                this.copy(
                    saksbehandlerIdent = saksbehandler.navIdent,
                    beslutterIdent = if (saksbehandler.navIdent == beslutterIdent) null else beslutterIdent,
                    status = TilbakekrevingBehandlingsstatus.UNDER_BEHANDLING,
                    sistEndret = nå,
                )
            }

            TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> {
                check(saksbehandler.navIdent != this.saksbehandlerIdent) {
                    "Beslutter (${saksbehandler.navIdent}) kan ikke være den samme som saksbehandleren (${this.saksbehandlerIdent})"
                }
                require(this.beslutterIdent == null) {
                    "Behandlingen har en eksisterende beslutter. For å overta behandlingen, bruk overta()"
                }
                this.copy(
                    beslutterIdent = saksbehandler.navIdent,
                    status = TilbakekrevingBehandlingsstatus.UNDER_GODKJENNING,
                    sistEndret = nå,
                )
            }

            TilbakekrevingBehandlingsstatus.UNDER_BEHANDLING,
            TilbakekrevingBehandlingsstatus.UNDER_GODKJENNING,
            -> throw IllegalStateException(
                "Kan ikke ta behandling som allerede er tatt. For å overta, bruk overta(). tilbakekrevingId: $id, status: $status",
            )

            TilbakekrevingBehandlingsstatus.OPPRETTET,
            TilbakekrevingBehandlingsstatus.AVSLUTTET,
            -> throw IllegalArgumentException(
                "Kan ikke ta behandling når behandlingen har status $status. tilbakekrevingId: $id",
            )
        }
    }
}
