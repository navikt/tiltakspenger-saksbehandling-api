package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
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

fun MeldekortBehandling.tilMeldekortBehandlingDTO(
    utbetalingsvedtak: Utbetalingsvedtak? = null,
): MeldekortBehandlingDTO {
    require(status != MeldekortBehandlingStatus.GODKJENT || utbetalingsvedtak != null) {
        "Utbetalingsvedtak må være satt for godkjente meldekortbehandlinger. sakId ${this.sakId}, behandlingId: $id"
    }

    return MeldekortBehandlingDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        brukersMeldekortId = brukersMeldekort?.id.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        godkjentTidspunkt = utbetalingsvedtak?.opprettet ?: iverksattTidspunkt,
        status = this.toStatusDTO(),
        erAvsluttet = erAvsluttet,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        begrunnelse = begrunnelse?.verdi,
        type = type.tilDTO(),
        attesteringer = attesteringer.toAttesteringDTO(),
        utbetalingsstatus = utbetalingsvedtak?.status?.toUtbetalingsstatusDTO() ?: this.tilUtbetalingsstatusDto(),
        periode = periode.toDTO(),
        dager = dager.tilMeldekortDagerDTO(),
        beregning = beregning?.tilMeldekortBeregningDTO(),
        avbrutt = avbrutt?.toAvbruttDTO(),
        simulering = simulering?.tilSimuleringDTO(),
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
