package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.UtbetalingskontrollDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilUtbetalingskontrollDTO
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.KanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilKanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilMeldingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO
import java.time.LocalDateTime

data class MeldekortbehandlingDTO(
    val id: String,
    val sakId: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val godkjentTidspunkt: LocalDateTime?,
    val status: MeldekortbehandlingStatusDTO,
    val erAvsluttet: Boolean,
    val navkontor: String,
    val navkontorNavn: String?,
    val begrunnelse: String?,
    val attesteringer: List<AttesteringDTO>,
    val utbetalingsstatus: UtbetalingsstatusDTO,
    /** Sammenhengende totalperiode på tvers av alle [meldeperioder]. */
    val periode: PeriodeDTO,
    /**
     * Én eller flere meldeperioder.
     * Sortert kronologisk på fra-og-med.
     */
    val meldeperioder: List<MeldeperiodebehandlingDTO>,
    val avbrutt: AvbruttDTO?,
    val simulertBeregning: SimulertBeregningDTO?,
    /**
     * Kontrollberegningen og -simuleringen som kjøres når behandlingen sendes videre i flyten.
     * Null dersom kontrollen ikke er kjørt enda.
     */
    val utbetalingskontroll: UtbetalingskontrollDTO?,
    val kanIkkeIverksetteUtbetaling: KanIkkeIverksetteUtbetalingDTO?,

    /**
     * Melding fra domenet som kan vises direkte til saksbehandler.
     * Null når grunnen alene er dekkende.
     */
    val kanIkkeIverksetteUtbetalingMelding: String?,
    val tekstTilVedtaksbrev: String?,
    val tilbakekrevingId: String?,
    val skalSendeVedtaksbrev: Boolean,
    /**
     * hendelsene er sortert desc
     */
    val ventestatus: List<VentestatusHendelseDTO>,
    val klagebehandlingId: String?,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
)

data class MeldeperiodebehandlingDTO(
    val meldeperiodeId: String,
    val kjedeId: String,
    /** Meldekortene fra bruker som denne meldeperiodebehandlingen behandler, sortert eldst først. */
    val brukersMeldekortIder: List<String>,
    val periode: PeriodeDTO,
    val dager: List<MeldekortDagDTO>,
    val beregning: MeldeperiodeBeregningDTO?,
    val type: MeldeperiodebehandlingTypeDTO,
)

fun Meldekortbehandling.tilMeldekortbehandlingDTO(
    beregninger: MeldeperiodeBeregningerVedtatt,
    hentVedtak: (id: MeldekortId) -> Meldekortvedtak?,
    hentTilbakekreving: (id: MeldekortId) -> TilbakekrevingBehandling?,
    kallendeSaksbehandler: Saksbehandler,
): MeldekortbehandlingDTO {
    val vedtak: Meldekortvedtak? = hentVedtak(id)

    require(status != MeldekortbehandlingStatus.GODKJENT || vedtak != null) {
        "Meldekortvedtak må finnes for godkjente meldekortbehandlinger. sakId ${this.sakId}, behandlingId: $id"
    }

    val kanIkkeIverksette = this.validerKanIverksetteUtbetaling().leftOrNull()
    return MeldekortbehandlingDTO(
        id = id.toString(),
        sakId = sakId.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = beslutter,
        opprettet = opprettet,
        sistEndret = sistEndret,
        godkjentTidspunkt = vedtak?.opprettet ?: iverksattTidspunkt,
        status = status.toStatusDTO(),
        erAvsluttet = erAvsluttet,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        begrunnelse = begrunnelse?.verdi,
        attesteringer = attesteringer.toAttesteringDTO(),
        utbetalingsstatus = vedtak?.utbetaling?.status?.toUtbetalingsstatusDTO() ?: this.tilUtbetalingsstatusDTO(),
        periode = meldeperioder.totalPeriode.toDTO(),
        meldeperioder = meldeperioder.meldeperioderMedBeregninger.map { it.tilMeldeperiodebehandlingDTO() },
        avbrutt = avbrutt?.toAvbruttDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger)?.toSimulertBeregningDTO(),
        utbetalingskontroll = utbetalingskontroll?.tilUtbetalingskontrollDTO(
            behandlingSimulering = simulering,
            beregninger = beregninger,
        ),
        kanIkkeIverksetteUtbetaling = kanIkkeIverksette?.tilKanIkkeIverksetteUtbetalingDTO(),
        kanIkkeIverksetteUtbetalingMelding = kanIkkeIverksette?.tilMeldingDTO(),
        tekstTilVedtaksbrev = fritekstTilVedtaksbrev?.verdi,
        tilbakekrevingId = hentTilbakekreving(id)?.id?.toString(),
        skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        ventestatus = ventestatus.ventestatusHendelser.tilDto(),
        klagebehandlingId = this.klagebehandling?.id?.toString(),
        gyldigeKommandoer = this.finnGyldigeKommandoer(kallendeSaksbehandler).tilDTO(),
    )
}

private fun MeldeperiodebehandlingMedBeregning.tilMeldeperiodebehandlingDTO(): MeldeperiodebehandlingDTO {
    return MeldeperiodebehandlingDTO(
        meldeperiodeId = meldeperiodebehandling.meldeperiodeId.toString(),
        kjedeId = meldeperiodebehandling.kjedeId.toString(),
        brukersMeldekortIder = meldeperiodebehandling.brukersMeldekort.map { it.id.toString() },
        periode = meldeperiodebehandling.periode.toDTO(),
        dager = meldeperiodebehandling.dager.tilMeldekortDagerDTO(),
        beregning = meldeperiodeberegning?.tilMeldeperiodeBeregningDTO(),
        type = meldeperiodebehandling.type.tilDTO(),
    )
}

private fun Meldekortbehandling.tilUtbetalingsstatusDTO(): UtbetalingsstatusDTO =
    when (this) {
        is MeldekortbehandlingAvbrutt -> UtbetalingsstatusDTO.AVBRUTT

        is MeldekortBehandletAutomatisk,
        is MeldekortbehandlingManuell,
        is MeldekortUnderBehandling,
        -> UtbetalingsstatusDTO.IKKE_GODKJENT
    }
