package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningMeldeperiode.SimulertBeregningDag
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
    val meldeperioder: Nel<SimulertBeregningMeldeperiode>,
    // TODO jah: Grav i hvorfor denne tilsynelatende ikke finnes.
    val beregningstidspunkt: LocalDateTime?,
    val simuleringstidspunkt: LocalDateTime?,
    val simuleringsdato: LocalDate?,
    val simuleringTotalBeløp: Int?,
) {
    val simuleringsdager: NonEmptyList<Simuleringsdag>? by lazy {
        meldeperioder.mapNotNull { it.simuleringsdager }.flatten().toNonEmptyListOrNull()
    }

    val beregning: BeregningBeløp by lazy {
        meldeperioder.map { it.beregning }.summer()
    }

    val forrigeBeregning: BeregningBeløp? by lazy {
        meldeperioder.mapNotNull { it.forrigeBeregning }.toNonEmptyListOrNull()?.summer()
    }

    val beregningEndring: BeregningBeløp by lazy {
        meldeperioder.map { it.beregningEndring }.summer()
    }

    data class BeregningBeløp(
        val ordinær: Int,
        val barnetillegg: Int,
        val total: Int,
    )

    /**
     * En simulering for en enkelt meldeperiode kan ikke både inneholde feilutbetaling og etterbetaling, siden de motposterer hverandre.
     */
    data class SimulertBeregningMeldeperiode(
        val kjedeId: MeldeperiodeKjedeId,
        val dager: Nel<SimulertBeregningDag>,
    ) {
        val simuleringsdager: NonEmptyList<Simuleringsdag>? by lazy {
            dager.mapNotNull { it.simuleringsdag }.toNonEmptyListOrNull()
        }

        val beregning: BeregningBeløp by lazy {
            dager.map { it.beregning }.summer()
        }

        val forrigeBeregning: BeregningBeløp? by lazy {
            dager.mapNotNull { it.forrigeBeregning }.toNonEmptyListOrNull()?.summer()
        }

        val beregningEndring: BeregningBeløp by lazy {
            dager.map { it.beregningEndring }.summer()
        }

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

            val beregning: BeregningBeløp by lazy {
                BeregningBeløp(
                    ordinær = beregningsdag.beløp,
                    barnetillegg = beregningsdag.beløpBarnetillegg,
                    total = beregningsdag.totalBeløp,
                )
            }

            val forrigeBeregning: BeregningBeløp? by lazy {
                forrigeBeregningsdag?.let {
                    BeregningBeløp(
                        ordinær = it.beløp,
                        barnetillegg = it.beløpBarnetillegg,
                        total = it.totalBeløp,
                    )
                }
            }

            val beregningEndring: BeregningBeløp by lazy {
                BeregningBeløp(
                    ordinær = beregning.ordinær - (forrigeBeregning?.ordinær ?: 0),
                    barnetillegg = beregning.barnetillegg - (forrigeBeregning?.barnetillegg ?: 0),
                    total = beregning.total - (forrigeBeregning?.total ?: 0),
                )
            }
        }
    }

    companion object {
        fun create(
            beregning: Beregning,
            eksisterendeBeregninger: MeldeperiodeBeregningerVedtatt,
            simulering: Simulering?,
        ): SimulertBeregning {
            val perMeldeperiode: Nel<SimulertBeregningMeldeperiode> =
                beregning.beregninger.map { meldeperiodeBeregning ->
                    val kjedeId = meldeperiodeBeregning.kjedeId

                    val forrigeBeregning: MeldeperiodeBeregning? =
                        eksisterendeBeregninger.hentForrigeBeregningForSimulering(meldeperiodeBeregning)

                    SimulertBeregningMeldeperiode(
                        kjedeId = kjedeId,
                        dager = meldeperiodeBeregning.dager.map { dag ->
                            SimulertBeregningDag(
                                dato = dag.dato,
                                simuleringsdag = simulering?.hentDag(dag.dato),
                                beregningsdag = dag,
                                forrigeBeregningsdag = forrigeBeregning?.hentDag(dag.dato),
                            )
                        },
                    )
                }

            return SimulertBeregning(
                beregningskilde = beregning.beregningKilde,
                meldeperioder = perMeldeperiode,
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

fun MeldeperiodeBeregningerVedtatt.hentForrigeBeregningForSimulering(meldeperiodeBeregning: MeldeperiodeBeregning): MeldeperiodeBeregning? {
    return hentForrigeBeregning(
        meldeperiodeBeregning.id,
        meldeperiodeBeregning.kjedeId,
    ).getOrElse {
        when (it) {
            MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.IngenBeregningerForKjede,
            MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.IngenTidligereBeregninger,
            -> null

            /**
             *  Dersom beregningen vi prøvde å finne forrige beregning til ikke finnes (men det finnes andre beregninger på kjeden),
             *  betyr det at denne beregningen ikke er iverksatt ennå, og forrige beregning er den gjeldende/sist iverksatte beregningen
             * */
            MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.BeregningFinnesIkke,
            -> gjeldendeBeregningPerKjede[meldeperiodeBeregning.kjedeId]
        }
    }
}

private fun NonEmptyList<SimulertBeregning.BeregningBeløp>.summer(): SimulertBeregning.BeregningBeløp {
    return SimulertBeregning.BeregningBeløp(
        ordinær = this.sumOf { it.ordinær },
        barnetillegg = this.sumOf { it.barnetillegg },
        total = this.sumOf { it.total },
    )
}
