package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import kotlin.collections.zipWithNext

sealed interface UtbetalingBeregning {
    val beregninger: NonEmptyList<MeldeperiodeBeregning>

    val fraOgMed: LocalDate get() = beregninger.first().fraOgMed
    val tilOgMed: LocalDate get() = beregninger.last().tilOgMed

    val periode: Periode get() = Periode(fraOgMed, tilOgMed)

    val dager: NonEmptyList<MeldeperiodeBeregningDag> get() = beregninger.flatMap { it.dager }

    val beregningKilde: BeregningKilde get() = beregninger.first().beregningKilde

    /**
     * Ordinær stønad, ikke med barnetillegg
     */
    val ordinærBeløp: Int get() = beregninger.beregnOrdinærBeløp()

    /**
     * Barnetillegg uten ordinær stønad
     */
    val barnetilleggBeløp: Int get() = beregninger.beregnBarnetilleggBeløp()

    /**
     * Ordinær stønad + barnetillegg
     */
    val totalBeløp: Int get() = beregninger.beregnTotalBeløp()

    fun hentDag(dato: LocalDate): MeldeperiodeBeregningDag?

    fun init() {
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
