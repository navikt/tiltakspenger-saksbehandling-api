package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 *  [tilbakeBehandlingId] BehandlingId i ekstern saksbehandlingsløsning for tilbakekreving
 *  [status] Status på behandlingen fra tilbakeløsningen
 *  [statusIntern] Vår interne status, som tar hensyn til om behandlingen er tildelt saksbehandler eller beslutter
 *  [url] URL til behandlingen i ekstern saksbehandlingsløsning for tilbakekreving
 *  [saksbehandlerIdent] Saksbehandler som har tatt behandlingen i vårt system. Håndheves ikke mot tilbakeløsningen, er kun ment for å hjelpe saksbehandlere med å fordele oppgaver
 *  [beslutterIdent] Beslutter som har tatt behandlingen i vårt system. Håndheves ikke mot tilbakeløsningen, er kun ment for å hjelpe saksbehandlere med å fordele oppgaver
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
    val statusIntern: TilbakekrevingBehandlingsstatusIntern by lazy {
        when (status) {
            TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingsstatusIntern.OPPRETTET

            TilbakekrevingBehandlingsstatus.TIL_BEHANDLING ->
                if (saksbehandlerIdent != null) {
                    TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
                } else {
                    TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING
                }

            TilbakekrevingBehandlingsstatus.TIL_GODKJENNING ->
                if (beslutterIdent != null) {
                    TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING
                } else {
                    TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING
                }

            TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingsstatusIntern.AVSLUTTET
        }
    }

    init {
        if (saksbehandlerIdent != null && beslutterIdent != null) {
            require(saksbehandlerIdent != beslutterIdent) {
                "Saksbehandler og beslutter kan ikke være samme person. tilbakekrevingId: $id, sakId: $sakId"
            }
        }
    }
}
