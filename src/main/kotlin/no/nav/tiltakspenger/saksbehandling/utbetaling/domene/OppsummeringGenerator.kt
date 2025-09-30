package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import java.time.LocalDate
import kotlin.collections.filter
import kotlin.math.abs

/**
 * https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md
 */
object OppsummeringGenerator {
    fun lagOppsummering(
        posteringerPerDag: Map<LocalDate, PosteringerForDag>,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        datoBeregnet: LocalDate,
        totalBeløp: Int,
    ): Simulering.Endring {
        val simuleringsperiode = Periode(
            fraOgMed = posteringerPerDag.values.minOf { it.dato },
            tilOgMed = posteringerPerDag.values.maxOf { it.dato },
        )
        // Merk at simuleringsperioden og meldeperiodene sin totale periode ikke trenger å være like.
        val aktuelleMeldeperioder = meldeperiodeKjeder.hentMeldeperioderForPeriode(simuleringsperiode)
        return Simulering.Endring(
            datoBeregnet = datoBeregnet,
            totalBeløp = totalBeløp,
            simuleringPerMeldeperiode = aktuelleMeldeperioder.mapNotNull { meldeperiode ->
                val simuleringsdager = meldeperiode.periode.tilDager().mapNotNull { dato ->
                    posteringerPerDag[dato]?.let { posteringerForDag ->
                        Simuleringsdag(
                            dato = dato,
                            tidligereUtbetalt = beregnTidligereUtbetalt(posteringerForDag),
                            nyUtbetaling = beregnNyttBeløp(posteringerForDag),
                            totalEtterbetaling = beregnEtterbetaling(posteringerForDag),
                            totalFeilutbetaling = beregnFeilutbetaling(posteringerForDag),
                            totalMotpostering = beregnMotposteringer(posteringerForDag),
                            totalTrekk = beregnTrekk(posteringerForDag),
                            totalJustering = beregnJustering(posteringerForDag),
                            harNegativJustering = harNegativJustering(posteringerForDag),
                            posteringsdag = posteringerForDag,
                        )
                    }
                }
                simuleringsdager.toNonEmptyListOrNull()?.let {
                    SimuleringForMeldeperiode(
                        meldeperiode = meldeperiode,
                        simuleringsdager = it,
                    )
                }
            }.toNonEmptyListOrNull()!!,
        )
    }

    private fun beregnTidligereUtbetalt(posteringer: PosteringerForDag): Int =
        abs(posteringer.summerBareNegativePosteringer(Posteringstype.YTELSE))

    private fun beregnNyttBeløp(posteringer: PosteringerForDag): Int =
        posteringer.summerBarePositivePosteringer(Posteringstype.YTELSE) - posteringer.summerBarePositivePosteringer(
            Posteringstype.FEILUTBETALING,
            KLASSEKODE_FEILUTBETALING,
        )

    private fun beregnEtterbetaling(posteringer: PosteringerForDag): Int {
        val justeringer: Int = beregnJustering(posteringer)
        val resultat = beregnNyttBeløp(posteringer) - beregnTidligereUtbetalt(posteringer)
        return if (justeringer < 0) {
            maxOf(resultat - abs(justeringer), 0)
        } else {
            maxOf(resultat, 0)
        }
    }

    private fun beregnFeilutbetaling(posteringer: PosteringerForDag): Int =
        maxOf(0, posteringer.summerBarePositivePosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_FEILUTBETALING))

    private fun beregnMotposteringer(posteringer: PosteringerForDag): Int =
        posteringer.summerPosteringer(Posteringstype.MOTPOSTERING)

    /** TREK i OS/UR. Kommer som positive posteringer. */
    private fun beregnTrekk(posteringer: PosteringerForDag): Int =
        posteringer.summerBarePositivePosteringer(Posteringstype.TREKK)

    /**
     * Dersom den er negativ for denne dagen, vil den redusere etterbetalingen. Vi får et innslag per positive justering på andre dager.
     * Dersom den er positiv for denne dagen, vil dagen være justert istedenfor at den fører til feilubetaling. Utregning: tidligere utbetalt = ny utbetaling + justering.
     * Dette gjelder FEILUTBETALING kombinert med KLASSEKODE_JUSTERING. Dette er "motregninger" som må sees på tvers av dager.
     * Disse vil komme uten en MOTP.
     */
    private fun beregnJustering(posteringer: PosteringerForDag): Int =
        posteringer.summerPosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_JUSTERING)

    private fun harNegativJustering(posteringer: PosteringerForDag): Boolean =
        posteringer.posteringer.any { it.type == Posteringstype.FEILUTBETALING && it.klassekode == KLASSEKODE_JUSTERING }

    private fun PosteringerForDag.summerBarePositivePosteringer(type: Posteringstype): Int =
        this.posteringer.filter { it.beløp > 0 && it.type == type }.sumOf { it.beløp }

    private fun PosteringerForDag.summerBareNegativePosteringer(type: Posteringstype): Int =
        this.posteringer.filter { it.beløp < 0 && it.type == type }.sumOf { it.beløp }

    private fun PosteringerForDag.summerBarePositivePosteringer(type: Posteringstype, klassekode: String): Int =
        this.posteringer.filter { it.beløp > 0 && it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    private fun PosteringerForDag.summerPosteringer(type: Posteringstype, klassekode: String): Int =
        this.posteringer.filter { it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    private fun PosteringerForDag.summerPosteringer(type: Posteringstype): Int =
        this.posteringer.filter { it.type == type }.sumOf { it.beløp }

    const val KLASSEKODE_JUSTERING = "KL_KODE_JUST_ARBYT"
    const val KLASSEKODE_FEILUTBETALING = "KL_KODE_FEIL_ARBYT"
}
