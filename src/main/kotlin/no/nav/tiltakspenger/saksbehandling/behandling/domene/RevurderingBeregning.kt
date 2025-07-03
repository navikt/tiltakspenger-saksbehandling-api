package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import java.time.LocalDate
import kotlin.collections.zipWithNext

data class RevurderingBeregning(
    val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : List<MeldeperiodeBeregning> by beregninger {
    val fraOgMed: LocalDate get() = this.first().fraOgMed
    val tilOgMed: LocalDate get() = this.last().tilOgMed
    val periode = Periode(fraOgMed, tilOgMed)

    init {
        require(beregninger.all { it.beregningKilde is MeldeperiodeBeregning.FraBehandling })
        require(beregninger.zipWithNext().all { (a, b) -> a.tilOgMed < b.fraOgMed }) {
            "Beregnede meldeperioder må være sortert og ikke ha overlapp - $beregninger"
        }
        beregninger.nonDistinctBy { it.kjedeId }.also {
            require(it.isEmpty()) {
                "Kan ikke ha mer enn en beregning for hver meldeperiodekjede på samme behandling - $beregninger"
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
