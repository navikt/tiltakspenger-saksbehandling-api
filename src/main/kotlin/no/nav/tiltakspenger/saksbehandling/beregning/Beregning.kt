package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import java.time.LocalDate
import kotlin.collections.first

/**
 * En beregning for en eller flere meldeperioder.
 * Vil enten ha [BeregningKilde.BeregningKildeMeldekort] eller [BeregningKilde.BeregningKildeBehandling] som kilde.
 * Vil kunne ha hull. Er sortert og uten overlapp. En meldeperiodekjede er representert maks en gang.
 */
data class Beregning(
    val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : List<MeldeperiodeBeregning> by beregninger {

    val fraOgMed: LocalDate = beregninger.first().fraOgMed
    val tilOgMed: LocalDate = beregninger.last().tilOgMed

    val periode: Periode = Periode(fraOgMed, tilOgMed)

    val dager: NonEmptyList<MeldeperiodeBeregningDag> by lazy { beregninger.flatMap { it.dager } }

    val beregningKilde: BeregningKilde = beregninger.first().beregningKilde

    /** Ordinær stønad, uten barnetillegg */
    val ordinærBeløp: Int by lazy { beregninger.beregnOrdinærBeløp() }

    /** Barnetillegg, uten ordinær stønad */
    val barnetilleggBeløp: Int get() = beregninger.beregnBarnetilleggBeløp()

    /** Ordinær stønad + barnetillegg */
    val totalBeløp: Int get() = beregninger.beregnTotalBeløp()

    val førsteMeldeperiodeBeregning: MeldeperiodeBeregning by lazy { beregninger.first() }
    val beregningerForPåfølgendePerioder: List<MeldeperiodeBeregning> by lazy { beregninger.drop(1) }

    fun hentDag(dato: LocalDate): MeldeperiodeBeregningDag? {
        return beregninger.mapNotNull { it.hentDag(dato) }.singleOrNullOrThrow()
    }

    init {
        require(beregninger.zipWithNext().all { (a, b) -> a.tilOgMed < b.fraOgMed }) {
            "Beregnede meldeperioder må være sortert og ikke ha overlapp - $beregninger"
        }

        require(beregninger.distinctBy { it.beregningKilde }.size == 1) {
            "Beregnede meldeperioder må ha samme meldekort eller behandling som kilde - $beregninger"
        }

        require(beregninger.nonDistinctBy { it.kjedeId }.isEmpty()) {
            "Kan ikke ha mer enn en beregning for hver meldeperiodekjede - $beregninger"
        }
    }
}
