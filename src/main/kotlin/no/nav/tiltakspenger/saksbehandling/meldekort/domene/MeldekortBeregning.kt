package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import kotlin.collections.first
import kotlin.collections.last

data class MeldekortBeregning(
    override val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : Utbetalingsberegning,
    List<MeldeperiodeBeregning> by beregninger {

    val beregningForMeldekortetsPeriode by lazy { beregninger.first() }
    val beregningerForPåfølgendePerioder by lazy { beregninger.drop(1) }

    val dagerFraMeldekortet by lazy { beregningForMeldekortetsPeriode.dager }

    init {
        require(beregninger.all { it.beregningKilde is MeldeperiodeBeregning.FraMeldekort })

        super.init()
    }
}

sealed interface Utbetalingsberegning {
    val beregninger: NonEmptyList<MeldeperiodeBeregning>

    val fraOgMed: LocalDate get() = beregninger.first().fraOgMed
    val tilOgMed: LocalDate get() = beregninger.last().tilOgMed

    val periode: Periode get() = Periode(fraOgMed, tilOgMed)

    val dager: NonEmptyList<MeldeperiodeBeregningDag> get() = beregninger.flatMap { it.dager }

    val beregningKilde: MeldeperiodeBeregning.BeregningKilde get() = beregninger.first().beregningKilde

    /**
     * Ordinær stønad, ikke med barnetillegg
     */
    val ordinærBeløp: Int get() = beregninger.sumOf { it.beregnTotalOrdinærBeløp() }

    /**
     * Barnetillegg uten ordinær stønad
     */
    val barnetilleggBeløp: Int get() = beregninger.sumOf { it.beregnTotalBarnetillegg() }

    /**
     * Ordinær stønad + barnetillegg
     */
    val totalBeløp: Int get() = ordinærBeløp + barnetilleggBeløp

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
