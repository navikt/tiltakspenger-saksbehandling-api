package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BeløpFørOgNåDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BeregningerSummertDTO
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningMeldeperiode.SimulertBeregningDag
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param behandlingId meldekortbehandlingId eller rammebehandlingId.
 * @param simuleringstidspunkt er tidspunktet simuleringen ble utført. Kan være null hvis simulering ikke er utført, eller simuleringen ble utført før vi la på dette feltet.
 * @param simuleringsdato kommer fra Økonomisystemet.
 * @param simuleringTotalBeløp kommer fra Økonomisystemet.
 */
data class SimulertBeregningDTO(
    val behandlingId: String,
    val behandlingstype: Behandlingstype,
    val meldeperioder: List<SimulertBeregningMeldeperiode>,
    val beregningstidspunkt: LocalDateTime?,
    val simuleringstidspunkt: LocalDateTime?,
    val simuleringsdato: LocalDate?,
    val simuleringTotalBeløp: Int?,
    val simulerteBeløp: SimulerteBeløp?,
    val simuleringResultat: SimuleringResultatDTO,
    val beregning: BeregningerSummertDTO,
    val utbetalingValideringsfeil: KanIkkeIverksetteUtbetalingDTO?,
) {

    enum class Behandlingstype {
        MELDEKORT,
        RAMME,
    }

    data class SimulerteBeløp(
        val feilutbetaling: Int,
        val etterbetaling: Int,
        val tidligereUtbetaling: Int,
        val nyUtbetaling: Int,
        val totalJustering: Int,
        val totalTrekk: Int,
    )

    data class SimulertBeregningMeldeperiode(
        val kjedeId: String,
        val dager: List<SimulertBeregningDag>,
        val simulerteBeløp: SimulerteBeløp?,
        val beregning: BeregningerSummertDTO,
    ) {

        data class SimulertBeregningDag(
            val dato: LocalDate,
            val status: MeldekortDagStatusDTO,
            val beregning: BeregningerSummertDTO,
            val simulerteBeløp: SimulerteBeløp?,
            val posteringer: List<PosteringForDagDTO>?,
        )
    }

    data class PosteringForDagDTO(
        val fagområde: String,
        val beløp: Int,
        val type: PosteringstypeDTO,
        val klassekode: String,
    )

    enum class PosteringstypeDTO {
        YTELSE,
        FEILUTBETALING,
        FORSKUDSSKATT,
        JUSTERING,
        TREKK,
        MOTPOSTERING,
    }

    enum class SimuleringResultatDTO {
        ENDRING,
        INGEN_ENDRING,
        IKKE_SIMULERT,
    }

    enum class KanIkkeIverksetteUtbetalingDTO {
        SimuleringMangler,
        FeilutbetalingStøttesIkke,
        JusteringStøttesIkke,
    }
}

fun SimulertBeregning.toSimulertBeregningDTO(): SimulertBeregningDTO {
    return SimulertBeregningDTO(
        behandlingId = this.beregningskilde.id.toString(),
        behandlingstype = when (this.beregningskilde) {
            is BeregningKilde.BeregningKildeBehandling -> SimulertBeregningDTO.Behandlingstype.RAMME
            is BeregningKilde.BeregningKildeMeldekort -> SimulertBeregningDTO.Behandlingstype.MELDEKORT
        },
        meldeperioder = this.meldeperioder.map { it.toDTO() }.toList(),
        beregningstidspunkt = this.beregningstidspunkt,
        simuleringstidspunkt = this.simuleringstidspunkt,
        simuleringsdato = this.simuleringsdato,
        simuleringTotalBeløp = this.simuleringTotalBeløp,
        simulerteBeløp = this.simuleringsdager?.tilSimulerteBeløpDTO(),
        beregning = this.beregning.tilBeregningerSummertDTO(this.forrigeBeregning),
        simuleringResultat = this.simuleringResultat.tilDTO(),
        utbetalingValideringsfeil = this.utbetalingValideringsfeil?.tilDTO(),
    )
}

fun SimulertBeregningMeldeperiode.toDTO(): SimulertBeregningDTO.SimulertBeregningMeldeperiode {
    return SimulertBeregningDTO.SimulertBeregningMeldeperiode(
        kjedeId = this.kjedeId.toString(),
        dager = this.dager.map { it.toDTO() }.toList(),
        simulerteBeløp = this.simuleringsdager?.tilSimulerteBeløpDTO(),
        beregning = this.beregning.tilBeregningerSummertDTO(this.forrigeBeregning),
    )
}

fun SimulertBeregningDag.toDTO(): SimulertBeregningDTO.SimulertBeregningMeldeperiode.SimulertBeregningDag {
    return SimulertBeregningDTO.SimulertBeregningMeldeperiode.SimulertBeregningDag(
        dato = this.dato,
        status = this.beregningsdag.tilMeldekortDagStatusDTO(),
        simulerteBeløp = this.simuleringsdag?.let {
            SimulertBeregningDTO.SimulerteBeløp(
                feilutbetaling = it.totalFeilutbetaling,
                etterbetaling = it.totalEtterbetaling,
                tidligereUtbetaling = it.tidligereUtbetalt,
                nyUtbetaling = it.nyUtbetaling,
                totalJustering = it.totalJustering,
                totalTrekk = it.totalTrekk,
            )
        },
        beregning = BeregningerSummertDTO(
            totalt = BeløpFørOgNåDTO(
                før = this.forrigeBeregningsdag?.totalBeløp,
                nå = this.beregningsdag.totalBeløp,
            ),
            ordinært = BeløpFørOgNåDTO(
                før = this.forrigeBeregningsdag?.beløp,
                nå = this.beregningsdag.beløp,
            ),
            barnetillegg = BeløpFørOgNåDTO(
                før = this.forrigeBeregningsdag?.beløpBarnetillegg,
                nå = this.beregningsdag.beløpBarnetillegg,
            ),
        ),
        posteringer = this.simuleringsdag?.let {
            it.posteringsdag.posteringer.map { postering ->
                SimulertBeregningDTO.PosteringForDagDTO(
                    fagområde = postering.fagområde,
                    beløp = postering.beløp,
                    type = postering.type.tilDTO(),
                    klassekode = postering.klassekode,
                )
            }
        },
    )
}

private fun NonEmptyList<Simuleringsdag>.tilSimulerteBeløpDTO(): SimulertBeregningDTO.SimulerteBeløp {
    return SimulertBeregningDTO.SimulerteBeløp(
        feilutbetaling = this.sumOf { it.totalFeilutbetaling },
        etterbetaling = this.sumOf { it.totalEtterbetaling },
        tidligereUtbetaling = this.sumOf { it.tidligereUtbetalt },
        nyUtbetaling = this.sumOf { it.nyUtbetaling },
        totalJustering = this.sumOf { it.totalJustering },
        totalTrekk = this.sumOf { it.totalTrekk },
    )
}

private fun SimulertBeregning.BeregningBeløp.tilBeregningerSummertDTO(forrigeBeregning: SimulertBeregning.BeregningBeløp?): BeregningerSummertDTO {
    return BeregningerSummertDTO(
        totalt = BeløpFørOgNåDTO(
            før = forrigeBeregning?.total,
            nå = this.total,
        ),
        ordinært = BeløpFørOgNåDTO(
            før = forrigeBeregning?.ordinær,
            nå = this.ordinær,
        ),
        barnetillegg = BeløpFørOgNåDTO(
            før = forrigeBeregning?.barnetillegg,
            nå = this.barnetillegg,
        ),
    )
}

private fun Posteringstype.tilDTO(): SimulertBeregningDTO.PosteringstypeDTO {
    return when (this) {
        Posteringstype.YTELSE -> SimulertBeregningDTO.PosteringstypeDTO.YTELSE
        Posteringstype.FEILUTBETALING -> SimulertBeregningDTO.PosteringstypeDTO.FEILUTBETALING
        Posteringstype.FORSKUDSSKATT -> SimulertBeregningDTO.PosteringstypeDTO.FORSKUDSSKATT
        Posteringstype.JUSTERING -> SimulertBeregningDTO.PosteringstypeDTO.JUSTERING
        Posteringstype.TREKK -> SimulertBeregningDTO.PosteringstypeDTO.TREKK
        Posteringstype.MOTPOSTERING -> SimulertBeregningDTO.PosteringstypeDTO.MOTPOSTERING
    }
}

private fun SimulertBeregning.SimuleringResultat.tilDTO(): SimulertBeregningDTO.SimuleringResultatDTO {
    return when (this) {
        SimulertBeregning.SimuleringResultat.ENDRING -> SimulertBeregningDTO.SimuleringResultatDTO.ENDRING
        SimulertBeregning.SimuleringResultat.INGEN_ENDRING -> SimulertBeregningDTO.SimuleringResultatDTO.INGEN_ENDRING
        SimulertBeregning.SimuleringResultat.IKKE_SIMULERT -> SimulertBeregningDTO.SimuleringResultatDTO.IKKE_SIMULERT
    }
}

private fun KanIkkeIverksetteUtbetaling.tilDTO(): SimulertBeregningDTO.KanIkkeIverksetteUtbetalingDTO {
    return when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> SimulertBeregningDTO.KanIkkeIverksetteUtbetalingDTO.SimuleringMangler
        KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke -> SimulertBeregningDTO.KanIkkeIverksetteUtbetalingDTO.FeilutbetalingStøttesIkke
        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> SimulertBeregningDTO.KanIkkeIverksetteUtbetalingDTO.JusteringStøttesIkke
    }
}
