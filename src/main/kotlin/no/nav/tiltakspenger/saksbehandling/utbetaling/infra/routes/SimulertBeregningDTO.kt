package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningPerMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningPerMeldeperiode.SimulertBeregningDag
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
    val perMeldeperiode: List<SimulertBeregningPerMeldeperiodeDTO>,
    val beregningstidspunkt: LocalDateTime?,
    val simuleringstidspunkt: LocalDateTime?,
    val simuleringsdato: LocalDate?,
    val simuleringTotalBeløp: Int?,
    val simuleringFeilutbetaling: Int?,
    val simuleringEtterbetaling: Int?,
    val simuleringTidligereUtbetalt: Int?,
    val simuleringNyUtbetaling: Int?,
    val simuleringTotalJustering: Int?,
    val simuleringTotalTrekk: Int?,
    val beregningEndring: BeløpDTO,
) {

    enum class Behandlingstype {
        MELDEKORT,
        RAMME,
    }

    data class SimulertBeregningPerMeldeperiodeDTO(
        val meldeperiodeKjedeId: String,
        val dager: List<SimulertBeregningDagDTO>,
        val simuleringFeilutbetaling: Int?,
        val simuleringEtterbetaling: Int?,
        val simuleringTidligereUtbetalt: Int?,
        val simuleringNyUtbetaling: Int?,
        val simuleringTotalJustering: Int?,
        val simuleringTotalTrekk: Int?,
        val beregningEndring: BeløpDTO,
        val beregning: BeløpDTO,
    ) {
        data class SimulertBeregningDagDTO(
            val dato: LocalDate,
            val simuleringFeilutbetaling: Int?,
            val simuleringEtterbetaling: Int?,
            val simuleringTidligereUtbetalt: Int?,
            val simuleringNyUtbetaling: Int?,
            val simuleringTotalJustering: Int?,
            val simuleringTotalTrekk: Int?,
            val beregningEndring: BeløpDTO,
            val beregningOrdinær: Int,
            val beregningBarnetillegg: Int,
            val beregningTotal: Int,
        )
    }
}

fun SimulertBeregning.toSimulertBeregningDTO(): SimulertBeregningDTO {
    return SimulertBeregningDTO(
        behandlingId = this.beregningskilde.id.toString(),
        behandlingstype = when (this.beregningskilde) {
            is BeregningKilde.BeregningKildeBehandling -> SimulertBeregningDTO.Behandlingstype.RAMME
            is BeregningKilde.BeregningKildeMeldekort -> SimulertBeregningDTO.Behandlingstype.MELDEKORT
        },
        perMeldeperiode = this.perMeldeperiode.map { it.toDTO() }.toList(),
        beregningstidspunkt = this.beregningstidspunkt,
        simuleringstidspunkt = this.simuleringstidspunkt,
        simuleringsdato = this.simuleringsdato,
        simuleringTotalBeløp = this.simuleringTotalBeløp,
        simuleringFeilutbetaling = this.simuleringTotalFeilutbetaling,
        simuleringEtterbetaling = this.simuleringTotalEtterbetaling,
        simuleringTidligereUtbetalt = this.simuleringTidligereUtbetalt,
        simuleringNyUtbetaling = this.simuleringNyUtbetaling,
        simuleringTotalJustering = this.simuleringTotalJustering,
        simuleringTotalTrekk = this.simuleringTotalTrekk,
        beregningEndring = this.beregningEndring.toDTO(),
    )
}

fun SimulertBeregningPerMeldeperiode.toDTO(): SimulertBeregningDTO.SimulertBeregningPerMeldeperiodeDTO {
    return SimulertBeregningDTO.SimulertBeregningPerMeldeperiodeDTO(
        meldeperiodeKjedeId = this.kjedeId.toString(),
        dager = this.dager.map { it.toDTO() }.toList(),
        simuleringFeilutbetaling = this.simuleringTotalFeilutbetaling,
        simuleringEtterbetaling = this.simuleringTotalEtterbetaling,
        simuleringTidligereUtbetalt = this.simuleringTidligereUtbetalt,
        simuleringNyUtbetaling = this.simuleringNyUtbetaling,
        simuleringTotalJustering = this.simuleringTotalJustering,
        simuleringTotalTrekk = this.simuleringTotalTrekk,
        beregningEndring = this.beregningEndring.toDTO(),
        beregning = BeløpDTO(
            ordinært = this.beregningOrdinær,
            barnetillegg = this.beregningBarnetillegg,
            totalt = this.beregningTotal,
        ),
    )
}

fun SimulertBeregningDag.toDTO(): SimulertBeregningDTO.SimulertBeregningPerMeldeperiodeDTO.SimulertBeregningDagDTO {
    return SimulertBeregningDTO.SimulertBeregningPerMeldeperiodeDTO.SimulertBeregningDagDTO(
        dato = this.dato,
        simuleringFeilutbetaling = this.simuleringsdag?.totalFeilutbetaling,
        simuleringEtterbetaling = this.simuleringsdag?.totalEtterbetaling,
        simuleringTidligereUtbetalt = this.simuleringsdag?.tidligereUtbetalt,
        simuleringNyUtbetaling = this.simuleringsdag?.nyUtbetaling,
        simuleringTotalJustering = this.simuleringsdag?.totalJustering,
        simuleringTotalTrekk = this.simuleringsdag?.totalTrekk,
        beregningEndring = this.beregningEndring.toDTO(),
        beregningOrdinær = this.beregningsdag.beløp,
        beregningBarnetillegg = this.beregningsdag.beløpBarnetillegg,
        beregningTotal = this.beregningsdag.totalBeløp,
    )
}

fun SimulertBeregningDag.BeregningEndring.toDTO(): BeløpDTO {
    return BeløpDTO(
        totalt = this.total,
        barnetillegg = this.barnetillegg,
        ordinært = this.ordinær,
    )
}
