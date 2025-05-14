package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.Endring.Detaljer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.Endring.Oppsummering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.Endring.OppsummeringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.Endring.PosteringType
import kotlin.collections.filter
import kotlin.math.abs

/**
 * https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md
 */
object OppsummeringGenerator {
    fun lagOppsummering(
        detaljer: Detaljer,
        meldeperiodeKjeder: MeldeperiodeKjeder,
    ): Oppsummering {
        val simuleringsperiode = detaljer.totalPeriode
        // Merk at simuleringsperioden og meldeperiodene sine totale periode ikke trenger å være like.
        val meldeperioder = meldeperiodeKjeder.hentMeldeperioderForPeriode(simuleringsperiode)
        val posteringer: List<Delperiode> =
            detaljer.perioder.toList().flatMap { it.delperioder }.filter { it.fagområde == "TILTAKSPENGER" }
        return meldeperioder.map { meldeperiode ->
            val posteringerForMeldeperiode = posteringer.mapNotNull { postering ->
                postering.periode.overlappendePeriode(meldeperiode.periode)?.let {
                    postering.copy(periode = it)
                }
            }
            val tidligereUtbetalt = beregnTidligereUtbetalt(posteringerForMeldeperiode)
            val nyUtbetaling = beregnNyttBeløp(posteringerForMeldeperiode)
            val totalEtterbetaling = beregnEtterbetaling(posteringerForMeldeperiode)
            val totalFeilutbetaling = beregnFeilutbetaling(posteringerForMeldeperiode)
            OppsummeringForMeldeperiode(
                meldeperiode = meldeperiode.periode,
                meldeperiodeKjedeId = meldeperiode.kjedeId,
                tidligereUtbetalt = tidligereUtbetalt,
                nyUtbetaling = nyUtbetaling,
                totalEtterbetaling = totalEtterbetaling,
                totalFeilutbetaling = totalFeilutbetaling,
            )
        }.let { oppsummeringPerMeldeperiode ->
            Oppsummering(
                periode = simuleringsperiode,
                tidligereUtbetalt = oppsummeringPerMeldeperiode.sumOf { it.tidligereUtbetalt },
                nyUtbetaling = oppsummeringPerMeldeperiode.sumOf { it.nyUtbetaling },
                totalEtterbetaling = oppsummeringPerMeldeperiode.sumOf { it.totalEtterbetaling },
                totalFeilutbetaling = oppsummeringPerMeldeperiode.sumOf { it.totalFeilutbetaling },
                perMeldeperiode = oppsummeringPerMeldeperiode.toNonEmptyListOrNull()!!,
            )
        }
    }

    private fun beregnTidligereUtbetalt(posteringer: List<Delperiode>): Int =
        abs(posteringer.summerBareNegativePosteringer(PosteringType.YTELSE))

    private fun beregnNyttBeløp(posteringer: List<Delperiode>): Int =
        posteringer.summerBarePositivePosteringer(PosteringType.YTELSE) - posteringer.summerBarePositivePosteringer(
            PosteringType.FEILUTBETALING,
            KLASSEKODE_FEILUTBETALING,
        )

    private fun beregnEtterbetaling(posteringer: List<Delperiode>): Int {
        val justeringer = posteringer.summerPosteringer(PosteringType.FEILUTBETALING, KLASSEKODE_JUSTERING)
        val resultat = beregnNyttBeløp(posteringer) - beregnTidligereUtbetalt(posteringer)
        return if (justeringer < 0) {
            maxOf(resultat - abs(justeringer), 0)
        } else {
            maxOf(resultat, 0)
        }
    }

    private fun beregnFeilutbetaling(posteringer: List<Delperiode>): Int =
        maxOf(0, posteringer.summerBarePositivePosteringer(PosteringType.FEILUTBETALING, KLASSEKODE_FEILUTBETALING))

    private fun List<Delperiode>.summerBarePositivePosteringer(type: PosteringType): Int =
        this.filter { it.beløp > 0 && it.type == type }.sumOf { it.beløp }

    private fun List<Delperiode>.summerBareNegativePosteringer(type: PosteringType): Int =
        this.filter { it.beløp < 0 && it.type == type }.sumOf { it.beløp }

    private fun List<Delperiode>.summerBarePositivePosteringer(type: PosteringType, klassekode: String): Int =
        this.filter { it.beløp > 0 && it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    private fun List<Delperiode>.summerPosteringer(type: PosteringType, klassekode: String): Int =
        this.filter { it.type == type && it.klassekode == klassekode }.sumOf { it.beløp }

    const val KLASSEKODE_JUSTERING = "KL_KODE_JUST_ARBYT"
    const val KLASSEKODE_FEILUTBETALING = "KL_KODE_FEIL_ARBYT"
}
