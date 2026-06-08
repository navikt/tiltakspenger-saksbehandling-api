package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.v2

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDagDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDagerDTO
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingMedBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingTypeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortDagerDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toStatusDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.KanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilKanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO
import java.time.LocalDateTime

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
    val avbrutt: AvbruttDTO?,
    val simulertBeregning: SimulertBeregningDTO?,
    val kanIkkeIverksetteUtbetaling: KanIkkeIverksetteUtbetalingDTO?,
    val tekstTilVedtaksbrev: String?,
    val tilbakekrevingId: String?,
    val skalSendeVedtaksbrev: Boolean,
    val ventestatus: List<VentestatusHendelseDTO>,
    val klagebehandlingId: String?,
)

data class MeldeperiodebehandlingDTO(
    val meldeperiodeId: String,
    val kjedeId: String,
    /** Foreløpig satt kun for automatiske behandlinger som er knyttet til et brukers meldekort. */
    val brukersMeldekortId: String?,
    val periode: PeriodeDTO,
    val dager: List<MeldekortDagDTO>,
    val beregning: MeldeperiodeBeregningDTOV2?,
)

fun Meldekortbehandling.tilMeldekortbehandlingDTOV2(
    beregninger: MeldeperiodeBeregningerVedtatt,
    hentVedtak: (id: MeldekortId) -> Meldekortvedtak?,
    hentTilbakekreving: (id: MeldekortId) -> TilbakekrevingBehandling?,
): MeldekortbehandlingDTOV2 {
    val vedtak: Meldekortvedtak? = hentVedtak(id)

    require(status != MeldekortbehandlingStatus.GODKJENT || vedtak != null) {
        "Meldekortvedtak må finnes for godkjente meldekortbehandlinger. sakId ${this.sakId}, behandlingId: $id"
    }

    return MeldekortbehandlingDTOV2(
        id = id.toString(),
        sakId = sakId.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        godkjentTidspunkt = vedtak?.opprettet ?: iverksattTidspunkt,
        status = status.toStatusDTO(),
        erAvsluttet = erAvsluttet,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        begrunnelse = begrunnelse?.verdi,
        type = type.tilDTO(),
        attesteringer = attesteringer.toAttesteringDTO(),
        utbetalingsstatus = vedtak?.utbetaling?.status?.toUtbetalingsstatusDTO() ?: this.tilUtbetalingsstatusDtoV2(),
        periode = meldeperioder.totalPeriode.toDTO(),
        meldeperioder = meldeperioder.meldeperioderMedBeregninger.map { it.tilMeldeperiodebehandlingDTO() },
        avbrutt = avbrutt?.toAvbruttDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger)?.toSimulertBeregningDTO(),
        kanIkkeIverksetteUtbetaling = this.validerKanIverksetteUtbetaling().leftOrNull()
            ?.tilKanIkkeIverksetteUtbetalingDTO(),
        tekstTilVedtaksbrev = fritekstTilVedtaksbrev?.verdi,
        tilbakekrevingId = hentTilbakekreving(id)?.id?.toString(),
        skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        ventestatus = ventestatus.ventestatusHendelser.tilDto(),
        klagebehandlingId = this.klagebehandling?.id?.toString(),
    )
}

fun MeldeperiodebehandlingMedBeregning.tilMeldeperiodebehandlingDTO(): MeldeperiodebehandlingDTO {
    return MeldeperiodebehandlingDTO(
        meldeperiodeId = meldeperiodebehandling.meldeperiodeId.toString(),
        kjedeId = meldeperiodebehandling.kjedeId.toString(),
        brukersMeldekortId = meldeperiodebehandling.brukersMeldekort?.id?.toString(),
        periode = meldeperiodebehandling.periode.toDTO(),
        dager = meldeperiodebehandling.dager.tilMeldekortDagerDTO(),
        beregning = meldeperiodeberegning?.tilMeldeperiodeBeregningDTOV2(),
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

data class MeldeperiodeBeregningDTOV2(
    val beløp: BeløpDTO,
    val dager: List<MeldeperiodeBeregningDagDTO>,
)

private fun MeldeperiodeBeregning.tilMeldeperiodeBeregningDTOV2(): MeldeperiodeBeregningDTOV2 {
    return MeldeperiodeBeregningDTOV2(
        beløp = BeløpDTO(
            totalt = totalBeløp,
            ordinært = ordinærBeløp,
            barnetillegg = barnetilleggBeløp,
        ),
        dager = this.tilMeldeperiodeBeregningDagerDTO(),
    )
}
