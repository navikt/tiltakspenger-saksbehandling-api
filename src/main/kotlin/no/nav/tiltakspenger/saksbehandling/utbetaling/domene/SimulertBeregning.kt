package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Nel
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.UtbetalingBeregning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningPerMeldeperiode.SimulertBeregningDag
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.Int

/**
 * I visse tilfeller der man simulerer en beregning, vil vi kunne få "ingen endring" som svar.
 * Deler inn modellen i 3 nivåer: 1) hele beregningen (en eller flere meldeperioder), 2) per meldeperiode, 3) per dag.
 * Dette støtter også opp om hvordan vi viser beregning og simulering i frontend.
 * Denne datamodellen utledes fra beregning og simulering og persisteres ikke direkte til databasen.
 *
 * @param beregningskilde gir mulighet til å utlede hvilken rammebehandling eller meldekortbehandling som utløste denne beregningen.
 * @param simuleringstidspunkt er tidspunktet simuleringen ble utført. Kan være null hvis simulering ikke er utført, eller simuleringen ble utført før vi la på dette feltet.
 * @param simuleringsdato kommer fra Økonomisystemet. Null dersom simuleringen ga ingen endring.
 * @param simuleringTotalBeløp kommer fra Økonomisystemet. Null dersom simuleringen ga ingen endring.
 */
data class SimulertBeregning(
    val beregningskilde: BeregningKilde,
    val perMeldeperiode: Nel<SimulertBeregningPerMeldeperiode>,
    // TODO jah: Grav i hvorfor denne tilsynelatende ikke finnes.
    val beregningstidspunkt: LocalDateTime?,
    val simuleringstidspunkt: LocalDateTime?,
    val simuleringsdato: LocalDate?,
    val simuleringTotalBeløp: Int?,
) {
    val simuleringTidligereUtbetalt: Int? by lazy {
        perMeldeperiode.mapNotNull { it.simuleringTidligereUtbetalt }.ifEmpty { null }?.sumOf { it }
    }
    val simuleringNyUtbetaling: Int? by lazy {
        perMeldeperiode.mapNotNull { it.simuleringNyUtbetaling }.ifEmpty { null }?.sumOf { it }
    }
    val simuleringTotalEtterbetaling: Int? by lazy {
        perMeldeperiode.mapNotNull { it.simuleringTotalEtterbetaling }.ifEmpty { null }?.sumOf { it }
    }
    val simuleringTotalFeilutbetaling: Int? by lazy {
        perMeldeperiode.mapNotNull { it.simuleringTotalFeilutbetaling }.ifEmpty { null }?.sumOf { it }
    }
    val simuleringTotalJustering: Int? by lazy {
        perMeldeperiode.mapNotNull { it.simuleringTotalJustering }.ifEmpty { null }?.sumOf { it }
    }
    val simuleringTotalTrekk: Int? by lazy {
        perMeldeperiode.mapNotNull { it.simuleringTotalTrekk }.ifEmpty { null }?.sumOf { it }
    }
    val beregningEndring by lazy {
        SimulertBeregningDag.BeregningEndring(
            total = perMeldeperiode.sumOf { it.beregningEndring.total },
            barnetillegg = perMeldeperiode.sumOf { it.beregningEndring.barnetillegg },
            ordinær = perMeldeperiode.sumOf { it.beregningEndring.ordinær },
        )
    }

    /**
     * En simulering for en enkelt meldeperiode kan ikke både inneholde feilutbetaling og etterbetaling, siden de motposterer hverandre.
     */
    data class SimulertBeregningPerMeldeperiode(
        val kjedeId: MeldeperiodeKjedeId,
        val dager: Nel<SimulertBeregningDag>,
    ) {
        private val simuleringsdager: List<Simuleringsdag> by lazy { dager.mapNotNull { it.simuleringsdag } }
        val simuleringTidligereUtbetalt: Int? by lazy {
            simuleringsdager.map { it.tidligereUtbetalt }.ifEmpty { null }?.sumOf { it }
        }
        val simuleringNyUtbetaling: Int? by lazy {
            simuleringsdager.map { it.nyUtbetaling }.ifEmpty { null }?.sumOf { it }
        }
        val simuleringTotalEtterbetaling: Int? by lazy {
            simuleringsdager.map { it.totalEtterbetaling }.ifEmpty { null }?.sumOf { it }
        }
        val simuleringTotalFeilutbetaling: Int? by lazy {
            simuleringsdager.map { it.totalFeilutbetaling }.ifEmpty { null }?.sumOf { it }
        }
        val simuleringTotalJustering: Int? by lazy {
            simuleringsdager.map { it.totalJustering }.ifEmpty { null }?.sumOf { it }
        }
        val simuleringTotalTrekk: Int? by lazy {
            simuleringsdager.map { it.totalTrekk }.ifEmpty { null }?.sumOf { it }
        }

        val beregningEndring: SimulertBeregningDag.BeregningEndring by lazy {
            SimulertBeregningDag.BeregningEndring(
                total = dager.sumOf { it.beregningEndring.total },
                barnetillegg = dager.sumOf { it.beregningEndring.barnetillegg },
                ordinær = dager.sumOf { it.beregningEndring.ordinær },
            )
        }

        val beregningOrdinær: Int by lazy { dager.sumOf { it.beregningsdag.beløp } }
        val beregningBarnetillegg: Int by lazy { dager.sumOf { it.beregningsdag.beløpBarnetillegg } }
        val beregningTotal: Int by lazy { dager.sumOf { it.beregningsdag.totalBeløp } }

        /**
         * En dag i en simulering, som kobler sammen data fra både simuleringen og beregningen.
         * @param forrigeBeregningsdag er null dersom vi ikke tidligere har beregnet denne dagen.
         * @property beregningEndring Merk at dette kun er endringen fra vår forrige beregning, men ikke nødvendigvis utbetalt enda. Oppdrag simulerer kun mot det som er sent til UR. Hvis vi sender en utbetaling til oppdrag beregnes den ikke før på kvelden. Siste utbetaling vil overskrive overlappende perioder. Eksempel: vi utbetaler 1000 kr og samme dag stanser vi den dagen, da vil simuleringen si "ingen endring", mens beregningsendringen vil si -1000 kr.
         */
        data class SimulertBeregningDag(
            val dato: LocalDate,
            val beregningsdag: MeldeperiodeBeregningDag,
            val forrigeBeregningsdag: MeldeperiodeBeregningDag?,
            val simuleringsdag: Simuleringsdag?,
        ) {
            val beregningEndring: BeregningEndring by lazy {
                beregningsdag.let {
                    BeregningEndring(
                        total = it.totalBeløp - (forrigeBeregningsdag?.totalBeløp ?: 0),
                        barnetillegg = it.beløpBarnetillegg - (forrigeBeregningsdag?.beløpBarnetillegg ?: 0),
                        ordinær = it.beløp - (forrigeBeregningsdag?.beløp ?: 0),
                    )
                }
            }

            data class BeregningEndring(
                val total: Int,
                val barnetillegg: Int,
                val ordinær: Int,
            )
        }
    }

    companion object {
        fun create(
            beregning: UtbetalingBeregning,
            tidligereUtbetalinger: Utbetalinger,
            simulering: Simulering?,
        ): SimulertBeregning {
            val perMeldeperiode: Nel<SimulertBeregningPerMeldeperiode> =
                beregning.beregninger.map { meldeperiodeBeregning ->
                    SimulertBeregningPerMeldeperiode(
                        kjedeId = meldeperiodeBeregning.kjedeId,
                        dager = meldeperiodeBeregning.dager.map { dag ->
                            SimulertBeregningDag(
                                dato = dag.dato,
                                simuleringsdag = simulering?.hentDag(dag.dato),
                                beregningsdag = dag,
                                forrigeBeregningsdag = tidligereUtbetalinger.hentSisteBeregningdagForDato(dag.dato),
                            )
                        },
                    )
                }

            return SimulertBeregning(
                beregningskilde = beregning.beregningKilde,
                perMeldeperiode = perMeldeperiode,
                // TODO jah: Grav i hvorfor denne tilsynelatende ikke finnes.
                beregningstidspunkt = null,
                // TODO jah: Legg på denne på simuleringen i domenet+basen
                simuleringstidspunkt = null,
                simuleringsdato = (simulering as? Simulering.Endring)?.datoBeregnet,
                simuleringTotalBeløp = (simulering as? Simulering.Endring)?.totalBeløp,
            )
        }
    }
}
