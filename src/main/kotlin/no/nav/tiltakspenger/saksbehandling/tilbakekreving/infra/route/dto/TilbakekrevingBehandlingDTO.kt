package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeregningKildeDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilBeregningKildeDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingBehandlingDTO(
    val id: String,
    val sakId: String,
    val utbetalingIder: List<String>,
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
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
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

private fun TilbakekrevingBehandlingsstatusIntern.tilDTO() = when (this) {
    TilbakekrevingBehandlingsstatusIntern.OPPRETTET -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.OPPRETTET
    TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.TIL_BEHANDLING
    TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.UNDER_BEHANDLING
    TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.TIL_GODKJENNING
    TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.UNDER_GODKJENNING
    TilbakekrevingBehandlingsstatusIntern.AVSLUTTET -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.AVSLUTTET
}

fun TilbakekrevingBehandling.tilTilbakekrevingBehandlingDTO(
    utbetalinger: List<VedtattUtbetaling>,
    saksbehandler: Saksbehandler,
): TilbakekrevingBehandlingDTO {
    require(utbetalinger.map { it.id }.toSet() == utbetalingIder.toSet()) {
        "Utbetalingene matcher ikke utbetalingIder for tilbakekrevingsbehandling $id"
    }
    // Bruker siste utbetaling for å vise beregningKilde - de øvrige er historikk.
    val sisteUtbetaling = utbetalinger.maxBy { it.opprettet }

    return TilbakekrevingBehandlingDTO(
        id = id.toString(),
        sakId = sakId.toString(),
        utbetalingIder = utbetalingIder.map { it.toString() },
        beregningKilde = sisteUtbetaling.beregningKilde.tilBeregningKildeDTO(),
        tilbakeBehandlingId = tilbakeBehandlingId,
        opprettet = opprettet,
        sistEndret = sistEndret,
        status = statusIntern.tilDTO(),
        url = url,
        kravgrunnlagTotalPeriode = kravgrunnlagTotalPeriode.toDTO(),
        totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
        varselSendt = varselSendt,
        saksbehandler = this.saksbehandler,
        beslutter = beslutter,
        gyldigeKommandoer = this.gyldigeKommandoer(saksbehandler).tilDTO(),
    )
}
