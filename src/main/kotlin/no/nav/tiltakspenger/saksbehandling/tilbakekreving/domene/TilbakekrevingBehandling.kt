package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import no.nav.tiltakspenger.libs.common.SakId
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

    init {
        if (saksbehandlerIdent != null && beslutterIdent != null) {
            require(saksbehandlerIdent != beslutterIdent) {
                "Saksbehandler og beslutter kan ikke være samme person. tilbakekrevingId: $id, sakId: $sakId"
            }
        }

//        when (status) {
//            TilbakekrevingBehandlingsstatus.OPPRETTET,
//            TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
//            -> {
//                require(saksbehandlerIdent == null) {
//                    "Tilbakekreving med status $status kan ikke ha saksbehandler satt. tilbakekrevingId: $id, sakId: $sakId"
//                }
//                require(beslutterIdent == null) {
//                    "Tilbakekreving med status $status kan ikke ha beslutter satt. tilbakekrevingId: $id, sakId: $sakId"
//                }
//            }
//
//            TilbakekrevingBehandlingsstatus.UNDER_BEHANDLING,
//            TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
//            -> {
//                requireNotNull(saksbehandlerIdent) {
//                    "Tilbakekreving med status $status må ha saksbehandler satt. tilbakekrevingId: $id, sakId: $sakId"
//                }
//                require(beslutterIdent == null) {
//                    "Tilbakekreving med status $status kan ikke ha beslutter satt. tilbakekrevingId: $id, sakId: $sakId"
//                }
//            }
//
//            TilbakekrevingBehandlingsstatus.UNDER_GODKJENNING,
//            TilbakekrevingBehandlingsstatus.AVSLUTTET,
//            -> {
//                requireNotNull(saksbehandlerIdent) {
//                    "Tilbakekreving med status $status må ha saksbehandler satt. tilbakekrevingId: $id, sakId: $sakId"
//                }
//                requireNotNull(beslutterIdent) {
//                    "Tilbakekreving med status $status må ha beslutter satt. tilbakekrevingId: $id, sakId: $sakId"
//                }
//            }
//        }
    }
}
