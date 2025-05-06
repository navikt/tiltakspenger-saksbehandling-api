package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimuleringDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilSimuleringDTO
import java.time.LocalDateTime

data class MeldekortBehandlingDTO(
    val id: String,
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
    val simulering: SimuleringDTO?,
)

fun Utbetalingsvedtak.toMeldekortBehandlingDTO(): MeldekortBehandlingDTO {
    val behandling = this.meldekortbehandling

    return MeldekortBehandlingDTO(
        id = behandling.id.toString(),
        meldeperiodeId = behandling.meldeperiode.id.toString(),
        brukersMeldekortId = behandling.brukersMeldekort?.id.toString(),
        saksbehandler = behandling.saksbehandler,
        beslutter = behandling.beslutter,
        opprettet = behandling.opprettet,
        godkjentTidspunkt = this.opprettet,
        status = behandling.toStatusDTO(),
        erAvsluttet = behandling.erAvsluttet,
        navkontor = behandling.navkontor.kontornummer,
        navkontorNavn = behandling.navkontor.kontornavn,
        begrunnelse = behandling.begrunnelse?.verdi,
        type = behandling.type.tilDTO(),
        attesteringer = behandling.attesteringer.toAttesteringDTO(),
        utbetalingsstatus = this.status.toUtbetalingsstatusDTO(),
        periode = behandling.beregningPeriode.toDTO(),
        dager = behandling.dager.tilMeldekortDagerDTO(),
        beregning = behandling.beregning.tilMeldekortBeregningDTO(),
        avbrutt = behandling.avbrutt?.toAvbruttDTO(),
        simulering = behandling.simulering?.tilSimuleringDTO(),
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
        brukersMeldekortId = brukersMeldekort?.id.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        godkjentTidspunkt = iverksattTidspunkt,
        status = this.toStatusDTO(),
        erAvsluttet = this.erAvsluttet,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        begrunnelse = begrunnelse?.verdi,
        type = type.tilDTO(),
        attesteringer = attesteringer.toAttesteringDTO(),
        utbetalingsstatus = utbetalingsstatus,
        periode = this.periode.toDTO(),
        dager = dager.tilMeldekortDagerDTO(),
        beregning = beregning?.tilMeldekortBeregningDTO(),
        avbrutt = avbrutt?.toAvbruttDTO(),
        simulering = simulering?.tilSimuleringDTO(),
    )
}
