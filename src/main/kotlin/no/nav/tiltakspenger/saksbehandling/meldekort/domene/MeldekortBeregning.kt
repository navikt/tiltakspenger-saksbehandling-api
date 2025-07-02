package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

data class MeldekortBeregning(
    val beregninger: NonEmptyList<MeldeperiodeBeregningFraMeldekort>,
) : List<MeldeperiodeBeregningFraMeldekort> by beregninger {
    val fraOgMed: LocalDate get() = this.first().fraOgMed
    val tilOgMed: LocalDate get() = this.last().tilOgMed
    val periode = Periode(fraOgMed, tilOgMed)

    val beregningForMeldekortetsPeriode by lazy { beregninger.first() }
    val beregningerForPåfølgendePerioder by lazy { beregninger.drop(1) }

    val dagerFraMeldekortet by lazy { beregningForMeldekortetsPeriode.dager }
    val alleDager by lazy { beregninger.flatMap { it.dager } }

    init {
        require(beregninger.zipWithNext().all { (a, b) -> a.tilOgMed < b.fraOgMed }) {
            "Beregnede meldeperioder må være sortert og ikke ha overlapp - $beregninger"
        }
        beregninger.nonDistinctBy { it.kjedeId }.also {
            require(it.isEmpty()) {
                "Kan ikke ha mer enn en beregning for hver meldeperiodekjede på samme meldekort - $beregninger"
            }
        }
    }

    /**
     * Ordinær stønad, ikke med barnetillegg
     */
    fun beregnTotalOrdinærBeløp(): Int = beregninger.sumOf { it.beregnTotalOrdinærBeløp() }

    /**
     * Barnetillegg uten ordinær stønad
     */
    fun beregnTotalBarnetillegg(): Int = beregninger.sumOf { it.beregnTotalBarnetillegg() }

    /**
     * Ordinær stønad + barnetillegg
     */
    fun beregnTotaltBeløp(): Int = beregninger.sumOf { it.beregnTotaltBeløp() }
}
