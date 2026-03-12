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
    val status: TilbakekrevingBehandlingsstatus,
    val url: String,
    val kravgrunnlagTotalPeriode: Periode,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val varselSendt: LocalDate?,
)
