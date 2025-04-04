package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

data class MeldekortBeregning(
    val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : List<MeldeperiodeBeregning> by beregninger {
    val fraOgMed: LocalDate get() = this.first().fraOgMed
    val tilOgMed: LocalDate get() = this.last().tilOgMed
    val periode = Periode(fraOgMed, tilOgMed)

    val dager = beregninger.flatMap { it.dager }

    init {
        require(beregninger.zipWithNext().all { (a, b) -> a.tilOgMed < b.fraOgMed }) {
            "Beregnede meldeperioder må være sortert og ikke ha overlapp - $beregninger"
        }
    }

    /**
     * Ordinær stønad, ikke med barnetillegg
     */
    fun beregnTotalOrdinærBeløp(): Int = beregninger.flatMap { it.dager }.sumOf { it.beregningsdag?.beløp ?: 0 }

    /**
     * Barnetillegg uten ordinær stønad
     */
    fun beregnTotalBarnetillegg(): Int =
        beregninger.flatMap { it.dager }.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }

    /**
     * Ordinær stønad + barnetillegg
     */
    fun beregnTotaltBeløp(): Int = beregnTotalOrdinærBeløp() + beregnTotalBarnetillegg()
}
