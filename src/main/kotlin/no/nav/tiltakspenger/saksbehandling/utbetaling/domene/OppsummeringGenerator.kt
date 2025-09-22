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
            eksternDatoBeregnet = datoBeregnet,
            eksterntTotalbeløp = totalBeløp,
            simuleringPerMeldeperiode = aktuelleMeldeperioder.mapNotNull { meldeperiode ->
                val simuleringsdager = meldeperiode.periode.tilDager().mapNotNull { dato ->
                    posteringerPerDag[dato]?.let { posteringerForDag ->
                        beregnSimuleringsdag(dato, posteringerForDag)
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

    private fun beregnSimuleringsdag(dato: LocalDate, posteringerForDag: PosteringerForDag): Simuleringsdag {
        return Simuleringsdag(
            dato = dato,
            barnetillegg = Simuleringsbeløp(
                tidligereUtbetalt = beregnTidligereUtbetalt(posteringerForDag, true),
                nyUtbetaling = beregnNyttBeløp(posteringerForDag, true),
                totalEtterbetaling = beregnEtterbetaling(posteringerForDag, true),
                totalFeilutbetaling = beregnFeilutbetaling(posteringerForDag),
                totalTrekk = beregnTrekk(posteringerForDag),
                totalJustering = beregnJustering(posteringerForDag),
            ),
            ordinær = Simuleringsbeløp(
                tidligereUtbetalt = beregnTidligereUtbetalt(posteringerForDag, false),
                nyUtbetaling = beregnNyttBeløp(posteringerForDag, false),
                totalEtterbetaling = beregnEtterbetaling(posteringerForDag, false),
                totalFeilutbetaling = beregnFeilutbetaling(posteringerForDag),
                totalTrekk = beregnTrekk(posteringerForDag),
                totalJustering = beregnJustering(posteringerForDag),
            ),
            posteringsdag = posteringerForDag,
        )
    }

    private fun beregnTidligereUtbetalt(posteringer: PosteringerForDag, erBarnetillegg: Boolean): Int {
        return abs(posteringer.summerBareNegativePosteringer(Posteringstype.YTELSE))
    }

    private fun beregnNyttBeløp(posteringer: PosteringerForDag, erBarnetillegg: Boolean): Int {
        return posteringer.summerBarePositivePosteringer(Posteringstype.YTELSE) - posteringer.summerBarePositivePosteringer(
            Posteringstype.FEILUTBETALING,
            KLASSEKODE_FEILUTBETALING,
        )
    }

    private fun beregnEtterbetaling(posteringer: PosteringerForDag, erBarnetillegg: Boolean): Int {
        val justeringer: Int = posteringer.summerPosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_JUSTERING)
        val resultat = beregnNyttBeløp(posteringer, erBarnetillegg) - beregnTidligereUtbetalt(posteringer, erBarnetillegg)
        return if (justeringer < 0) {
            maxOf(resultat - abs(justeringer), 0)
        } else {
            maxOf(resultat, 0)
        }
    }

    /** Vi kan ikke lese ut fra FEILUTBETALING posteringene om de er ordinær, barnetillegg eller en kombinasjon. */
    private fun beregnFeilutbetaling(posteringer: PosteringerForDag): Int =
        maxOf(0, posteringer.summerBarePositivePosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_FEILUTBETALING))

    // TODO jah: Jeg vet ikke om disse kommer som positive eller negative fra helved. Gjetter på at disse er negative, men vi må bekrefte dette når vi har en simulering med trekk.
    private fun beregnTrekk(posteringer: PosteringerForDag): Int =
        posteringer.summerBareNegativePosteringer(Posteringstype.TREKK)

    /** TODO jah: Usikker på om disse vil være negative, positive eller en blanding. */
    private fun beregnJustering(posteringer: PosteringerForDag): Int =
        posteringer.summerPosteringer(Posteringstype.FEILUTBETALING, KLASSEKODE_JUSTERING)

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
