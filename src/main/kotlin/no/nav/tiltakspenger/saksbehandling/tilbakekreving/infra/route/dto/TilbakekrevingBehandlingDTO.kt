package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeregningKildeDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilBeregningKildeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingBehandlingDTO(
    val id: String,
    val sakId: String,
    val utbetalingId: String,
    val beregningKilde: BeregningKildeDTO,
    val tilbakeBehandlingId: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val status: TilbakekrevingBehandlingsstatusDTO,
    val url: String,
    val kravgrunnlagTotalPeriode: PeriodeDTO,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val varselSendt: LocalDate?,
    val saksbehandler: String?,
    val beslutter: String?,
) {

    enum class TilbakekrevingBehandlingsstatusDTO {
        OPPRETTET,
        TIL_BEHANDLING,
        UNDER_BEHANDLING,
        TIL_GODKJENNING,
        UNDER_GODKJENNING,
        AVSLUTTET,
    }
}

private fun TilbakekrevingBehandlingsstatus.tilDTO() = when (this) {
    TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.OPPRETTET
    TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.TIL_BEHANDLING
    TilbakekrevingBehandlingsstatus.UNDER_BEHANDLING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.UNDER_BEHANDLING
    TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.TIL_GODKJENNING
    TilbakekrevingBehandlingsstatus.UNDER_GODKJENNING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.UNDER_GODKJENNING
    TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.AVSLUTTET
}

fun TilbakekrevingBehandling.tilTilbakekrevingBehandlingDTO(utbetaling: VedtattUtbetaling): TilbakekrevingBehandlingDTO {
    require(utbetaling.id == utbetalingId)

    return TilbakekrevingBehandlingDTO(
        id = id.toString(),
        sakId = sakId.toString(),
        utbetalingId = utbetalingId.toString(),
        beregningKilde = utbetaling.beregningKilde.tilBeregningKildeDTO(),
        tilbakeBehandlingId = tilbakeBehandlingId,
        opprettet = opprettet,
        sistEndret = sistEndret,
        status = status.tilDTO(),
        url = url,
        kravgrunnlagTotalPeriode = kravgrunnlagTotalPeriode.toDTO(),
        totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
        varselSendt = varselSendt,
        saksbehandler = saksbehandlerIdent,
        beslutter = beslutterIdent,
    )
}
