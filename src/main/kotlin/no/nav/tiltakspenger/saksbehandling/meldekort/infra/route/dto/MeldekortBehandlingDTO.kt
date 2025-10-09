package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregninger
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.AvbruttMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO
import java.time.LocalDateTime

data class MeldekortBehandlingDTO(
    val id: String,
    val sakId: String,
    val meldeperiodeId: String,
    val brukersMeldekortId: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val opprettet: LocalDateTime,
    val godkjentTidspunkt: LocalDateTime?,
    val status: MeldekortBehandlingStatusDTO,
    val erAvsluttet: Boolean,
    val navkontor: String,
    val navkontorNavn: String?,
    val begrunnelse: String?,
    val type: MeldekortBehandlingTypeDTO,
    val attesteringer: List<AttesteringDTO>,
    val utbetalingsstatus: UtbetalingsstatusDTO,
    val periode: PeriodeDTO,
    val dager: List<MeldekortDagDTO>,
    val beregning: MeldekortBeregningDTO?,
    val avbrutt: AvbruttDTO?,
    val simulertBeregning: SimulertBeregningDTO?,
)

fun MeldekortBehandling.tilMeldekortBehandlingDTO(
    vedtak: MeldekortVedtak? = null,
    beregninger: MeldeperiodeBeregninger,
): MeldekortBehandlingDTO {
    require(status != MeldekortBehandlingStatus.GODKJENT || vedtak != null) {
        "Meldekortvedtak må være satt for godkjente meldekortbehandlinger. sakId ${this.sakId}, behandlingId: $id"
    }

    return MeldekortBehandlingDTO(
        id = id.toString(),
        sakId = sakId.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        brukersMeldekortId = brukersMeldekort?.id.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        godkjentTidspunkt = vedtak?.opprettet ?: iverksattTidspunkt,
        status = this.toStatusDTO(),
        erAvsluttet = erAvsluttet,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        begrunnelse = begrunnelse?.verdi,
        type = type.tilDTO(),
        attesteringer = attesteringer.toAttesteringDTO(),
        utbetalingsstatus = vedtak?.utbetaling?.status?.toUtbetalingsstatusDTO() ?: this.tilUtbetalingsstatusDto(),
        periode = periode.toDTO(),
        dager = dager.tilMeldekortDagerDTO(),
        beregning = beregning?.tilMeldekortBeregningDTO(),
        avbrutt = avbrutt?.toAvbruttDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger)?.toSimulertBeregningDTO(),
    )
}

private fun MeldekortBehandling.tilUtbetalingsstatusDto(): UtbetalingsstatusDTO {
    return when (this) {
        is AvbruttMeldekortBehandling -> UtbetalingsstatusDTO.AVBRUTT
        is MeldekortBehandletAutomatisk -> UtbetalingsstatusDTO.IKKE_GODKJENT
        is MeldekortBehandletManuelt -> UtbetalingsstatusDTO.IKKE_GODKJENT
        is MeldekortUnderBehandling -> UtbetalingsstatusDTO.IKKE_GODKJENT
    }
}
