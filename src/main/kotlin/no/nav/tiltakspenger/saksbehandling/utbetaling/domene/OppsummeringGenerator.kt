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
            simuleringPerMeldeperiode = aktuelleMeldeperioder.map { meldeperiode ->
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode,
                    simuleringsdager = meldeperiode.periode.tilDager().mapNotNull { dato ->
                        posteringerPerDag[dato]?.let { posteringerForDag ->
                            Simuleringsdag(
                                dato = dato,
                                tidligereUtbetalt = beregnTidligereUtbetalt(posteringerForDag),
                                nyUtbetaling = beregnNyttBeløp(posteringerForDag),
                                totalEtterbetaling = beregnEtterbetaling(posteringerForDag),
                                totalFeilutbetaling = beregnFeilutbetaling(posteringerForDag),
                                totalTrekk = beregnTrekk(posteringerForDag),
                                totalJustering = beregnJustering(posteringerForDag),
                                posteringsdag = posteringerForDag,
                            )
                        }
                    }.toNonEmptyListOrNull()!!,
                )
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
        val justeringer: Int = posteringer.summerPosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_JUSTERING)
        val resultat = beregnNyttBeløp(posteringer) - beregnTidligereUtbetalt(posteringer)
        return if (justeringer < 0) {
            maxOf(resultat - abs(justeringer), 0)
        } else {
            maxOf(resultat, 0)
        }
    }

    private fun beregnFeilutbetaling(posteringer: PosteringerForDag): Int =
        maxOf(0, posteringer.summerBarePositivePosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_FEILUTBETALING))

    // TODO jah: Jeg vet ikke om disse kommer som positive eller negative fra helved. Gjetter på at disse er negative, men vi må bekrefte dette når vi har en simulering med trekk.
    private fun beregnTrekk(posteringer: PosteringerForDag): Int = posteringer.summerBareNegativePosteringer(Posteringstype.TREKK)

    /** TODO jah: Usikker på om disse vil være negative, postive eller begge deler. */
    private fun beregnJustering(posteringer: PosteringerForDag): Int = posteringer.summerPosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_JUSTERING)

    private fun PosteringerForDag.summerBarePositivePosteringer(type: Posteringstype): Int =
        this.posteringer.filter { it.beløp > 0 && it.type == type }.sumOf { it.beløp }

    private fun PosteringerForDag.summerBareNegativePosteringer(type: Posteringstype): Int =
        this.posteringer.filter { it.beløp < 0 && it.type == type }.sumOf { it.beløp }

    private fun PosteringerForDag.summerBarePositivePosteringer(type: Posteringstype, klassekode: String): Int =
        this.posteringer.filter { it.beløp > 0 && it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    private fun PosteringerForDag.summerPosteringer(type: Posteringstype, klassekode: String): Int =
        this.posteringer.filter { it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    const val KLASSEKODE_JUSTERING = "KL_KODE_JUST_ARBYT"
    const val KLASSEKODE_FEILUTBETALING = "KL_KODE_FEIL_ARBYT"
}
