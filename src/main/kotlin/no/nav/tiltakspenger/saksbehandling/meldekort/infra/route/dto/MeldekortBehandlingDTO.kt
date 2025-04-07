package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import java.time.LocalDateTime

data class MeldekortBehandlingDTO(
    val id: String,
    val meldeperiodeId: String,
    val saksbehandler: String,
    val beslutter: String?,
    val opprettet: LocalDateTime,
    val status: MeldekortBehandlingStatusDTO,
    val totalbeløpTilUtbetaling: Int?,
    val totalOrdinærBeløpTilUtbetaling: Int?,
    val totalBarnetilleggTilUtbetaling: Int?,
    val navkontor: String,
    val navkontorNavn: String?,
    val brukersMeldekortId: String?,
    val type: MeldekortBehandlingTypeDTO,
    val begrunnelse: String?,
    val attesteringer: List<AttesteringDTO>,
    val utbetalingsstatus: UtbetalingsstatusDTO,
    val godkjentTidspunkt: LocalDateTime?,
    val periode: PeriodeDTO,
    val dager: List<MeldekortDagDTO>,
    val dagerBeregnet: List<MeldeperiodeBeregningDagDTO>?,
    val beregning: List<MeldeperiodeBeregningDTO>?,
    val korrigeringer: List<MeldeperiodeKorrigeringDTO>?,
)

fun Utbetalingsvedtak.toMeldekortBehandlingDTO(): MeldekortBehandlingDTO {
    val behandling = this.meldekortbehandling
    return MeldekortBehandlingDTO(
        id = behandling.id.toString(),
        meldeperiodeId = behandling.meldeperiode.id.toString(),
        saksbehandler = behandling.saksbehandler,
        beslutter = behandling.beslutter,
        opprettet = behandling.opprettet,
        status = behandling.toStatusDTO(),
        totalbeløpTilUtbetaling = behandling.beløpTotal,
        totalOrdinærBeløpTilUtbetaling = behandling.ordinærBeløp,
        totalBarnetilleggTilUtbetaling = behandling.barnetilleggBeløp,
        navkontor = behandling.navkontor.kontornummer,
        navkontorNavn = behandling.navkontor.kontornavn,
        brukersMeldekortId = behandling.brukersMeldekort?.id.toString(),
        type = behandling.type.tilDTO(),
        begrunnelse = behandling.begrunnelse?.verdi,
        attesteringer = behandling.attesteringer.toAttesteringDTO(),
        utbetalingsstatus = this.status.toUtbetalingsstatusDTO(),
        godkjentTidspunkt = this.opprettet,
        periode = behandling.beregningPeriode.toDTO(),
        dager = behandling.dager.tilMeldekortDagerDTO(),
        dagerBeregnet = behandling.beregning.dagerFraMeldekortet.toMeldeperiodeBeregningDagerDTO(),
        beregning = behandling.beregning.tilMeldekortBeregningDTO(),
        korrigeringer = emptyList(),
    )
}

fun MeldekortBehandling.toMeldekortBehandlingDTO(
    utbetalingsstatus: UtbetalingsstatusDTO,
): MeldekortBehandlingDTO {
    require(status != MeldekortBehandlingStatus.GODKJENT) {
        "Bruk Utbetalingsvedtak.toMeldekortBehandlingDTO() for godkjente meldekortbehandlinger. sakId ${this.sakId}, behandlingId: $id"
    }
    return MeldekortBehandlingDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        status = this.toStatusDTO(),
        totalbeløpTilUtbetaling = this.beløpTotal,
        totalOrdinærBeløpTilUtbetaling = this.ordinærBeløp,
        totalBarnetilleggTilUtbetaling = this.barnetilleggBeløp,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        brukersMeldekortId = brukersMeldekort?.id.toString(),
        type = type.tilDTO(),
        begrunnelse = begrunnelse?.verdi,
        attesteringer = attesteringer.toAttesteringDTO(),
        godkjentTidspunkt = iverksattTidspunkt,
        periode = this.periode.toDTO(),
        utbetalingsstatus = utbetalingsstatus,
        dager = dager.tilMeldekortDagerDTO(),
        dagerBeregnet = beregning?.dagerFraMeldekortet?.toMeldeperiodeBeregningDagerDTO(),
        beregning = beregning?.tilMeldekortBeregningDTO(),
        korrigeringer = emptyList(),
    )
}
