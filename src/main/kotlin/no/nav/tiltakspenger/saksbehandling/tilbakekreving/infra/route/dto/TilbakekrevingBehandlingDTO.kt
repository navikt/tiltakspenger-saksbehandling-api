package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingBehandlingDTO(
    val id: String,
    val sakId: String,
    val utbetalingId: String,
    val tilbakeBehandlingId: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val status: TilbakekrevingBehandlingsstatusDTO,
    val url: String,
    val kravgrunnlagTotalPeriode: PeriodeDTO,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val varselSendt: LocalDate?,
) {

    enum class TilbakekrevingBehandlingsstatusDTO {
        OPPRETTET,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
    }
}

private fun TilbakekrevingBehandlingsstatus.tilDTO() = when (this) {
    TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.OPPRETTET
    TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.TIL_BEHANDLING
    TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.TIL_GODKJENNING
    TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO.AVSLUTTET
}

fun TilbakekrevingBehandling.tilTilbakekrevingBehandlingDTO(): TilbakekrevingBehandlingDTO {
    return TilbakekrevingBehandlingDTO(
        id = id.toString(),
        sakId = sakId.toString(),
        utbetalingId = utbetalingId.toString(),
        tilbakeBehandlingId = tilbakeBehandlingId,
        opprettet = opprettet,
        sistEndret = sistEndret,
        status = status.tilDTO(),
        url = url,
        kravgrunnlagTotalPeriode = kravgrunnlagTotalPeriode.toDTO(),
        totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
        varselSendt = varselSendt,
    )
}
