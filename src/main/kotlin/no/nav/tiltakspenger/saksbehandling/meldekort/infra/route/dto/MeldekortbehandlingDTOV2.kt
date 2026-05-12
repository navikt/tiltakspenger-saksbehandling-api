package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilDto
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingAvbrutt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.KanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilKanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO
import java.time.LocalDateTime

/**
 * V2 av [MeldekortbehandlingDTO]. Forskjellen fra V1 er at vi her støtter at en behandling kan
 * inneholde flere meldeperioder. De per-meldeperiode-spesifikke feltene er flyttet inn i
 * [MeldeperiodebehandlingDTO]. Felter som [beregning] og [simulertBeregning] forblir på toppnivå
 * fordi de gjelder hele meldekortbehandlingen samlet.
 *
 * V1 vil leve videre i en overgangsperiode for klienter som ennå ikke har migrert.
 */
data class MeldekortbehandlingDTOV2(
    val id: String,
    val sakId: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val opprettet: LocalDateTime,
    val godkjentTidspunkt: LocalDateTime?,
    val status: MeldekortbehandlingStatusDTO,
    val erAvsluttet: Boolean,
    val navkontor: String,
    val navkontorNavn: String?,
    val begrunnelse: String?,
    val type: MeldekortbehandlingTypeDTO,
    val attesteringer: List<AttesteringDTO>,
    val utbetalingsstatus: UtbetalingsstatusDTO,
    /** Sammenhengende totalperiode på tvers av alle [meldeperioder]. */
    val periode: PeriodeDTO,
    /** Én eller flere meldeperioder. Sortert kronologisk på fra-og-med. */
    val meldeperioder: List<MeldeperiodebehandlingDTO>,
    val beregning: MeldekortBeregningDTO?,
    val avbrutt: AvbruttDTO?,
    val simulertBeregning: SimulertBeregningDTO?,
    val kanIkkeIverksetteUtbetaling: KanIkkeIverksetteUtbetalingDTO?,
    val tekstTilVedtaksbrev: String?,
    val tilbakekrevingId: String?,
    val skalSendeVedtaksbrev: Boolean,
    val ventestatus: List<VentestatusHendelseDTO>,
)

data class MeldeperiodebehandlingDTO(
    val meldeperiodeId: String,
    val kjedeId: String,
    /** Foreløpig satt kun for automatiske behandlinger som er knyttet til et brukers meldekort. */
    val brukersMeldekortId: String?,
    val periode: PeriodeDTO,
    val dager: List<MeldekortDagDTO>,
)

fun Meldekortbehandling.tilMeldekortbehandlingDTOV2(
    beregninger: MeldeperiodeBeregningerVedtatt,
    vedtak: Meldekortvedtak? = null,
    tilbakekreving: TilbakekrevingBehandling? = null,
): MeldekortbehandlingDTOV2 {
    require(status != MeldekortbehandlingStatus.GODKJENT || vedtak != null) {
        "Meldekortvedtak må være satt for godkjente meldekortbehandlinger. sakId ${this.sakId}, behandlingId: $id"
    }

    return MeldekortbehandlingDTOV2(
        id = id.toString(),
        sakId = sakId.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        godkjentTidspunkt = vedtak?.opprettet ?: iverksattTidspunkt,
        status = this.status.toStatusDTO(),
        erAvsluttet = erAvsluttet,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        begrunnelse = begrunnelse?.verdi,
        type = type.tilDTO(),
        attesteringer = attesteringer.toAttesteringDTO(),
        utbetalingsstatus = vedtak?.utbetaling?.status?.toUtbetalingsstatusDTO() ?: this.tilUtbetalingsstatusDtoV2(),
        periode = meldeperioder.totalPeriode.toDTO(),
        meldeperioder = meldeperioder.map { it.tilMeldeperiodebehandlingDTO() },
        beregning = beregning?.tilMeldekortBeregningDTO(),
        avbrutt = avbrutt?.toAvbruttDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger)?.toSimulertBeregningDTO(),
        kanIkkeIverksetteUtbetaling = this.validerKanIverksetteUtbetaling().leftOrNull()
            ?.tilKanIkkeIverksetteUtbetalingDTO(),
        tekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        tilbakekrevingId = tilbakekreving?.id?.toString(),
        skalSendeVedtaksbrev = this.skalSendeVedtaksbrev,
        ventestatus = this.ventestatus.ventestatusHendelser.tilDto(),
    )
}

private fun Meldeperiodebehandling.tilMeldeperiodebehandlingDTO(): MeldeperiodebehandlingDTO {
    return MeldeperiodebehandlingDTO(
        meldeperiodeId = meldeperiodeId.toString(),
        kjedeId = kjedeId.toString(),
        brukersMeldekortId = brukersMeldekort?.id?.toString(),
        periode = periode.toDTO(),
        dager = dager.tilMeldekortDagerDTO(),
    )
}

private fun Meldekortbehandling.tilUtbetalingsstatusDtoV2(): UtbetalingsstatusDTO =
    when (this) {
        is MeldekortbehandlingAvbrutt -> UtbetalingsstatusDTO.AVBRUTT

        is MeldekortBehandletAutomatisk,
        is MeldekortbehandlingManuell,
        is MeldekortUnderBehandling,
        -> UtbetalingsstatusDTO.IKKE_GODKJENT
    }
