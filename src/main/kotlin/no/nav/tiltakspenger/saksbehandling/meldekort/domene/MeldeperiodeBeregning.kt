package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate

data class MeldeperiodeBeregning(
    /** Meldekortbehandlingen som utløste denne beregningen */
    val meldekortId: MeldekortId,
    val kjedeId: MeldeperiodeKjedeId,
    val dager: NonEmptyList<MeldeperiodeBeregningDag>,
) : List<MeldeperiodeBeregningDag> by dager {
    val fraOgMed: LocalDate get() = dager.first().dato
    val tilOgMed: LocalDate get() = dager.last().dato
    val periode = Periode(fraOgMed, tilOgMed)

    init {
        require(dager.size == 14) { "En meldeperiode må være 14 dager, men var ${dager.size}" }
        require(dager.first().dato.dayOfWeek == DayOfWeek.MONDAY) { "Meldeperioden må starte på en mandag" }
        require(dager.last().dato.dayOfWeek == DayOfWeek.SUNDAY) { "Meldeperioden må slutte på en søndag" }
        dager.forEachIndexed { index, dag ->
            require(dager.first().dato.plusDays(index.toLong()) == dag.dato) {
                "Datoene må være sammenhengende og sortert, men var ${dager.map { it.dato }}"
            }
        }
        require(
            dager.zipWithNext()
                .all { (a, b) -> a.meldekortId == b.meldekortId },
        ) { "Alle dager må tilhøre samme meldekort, men var: ${dager.map { it.meldekortId }}" }
    }

    fun beregnTotalOrdinærBeløp(): Int = this.sumOf { it.beregningsdag?.beløp ?: 0 }

    fun beregnTotalBarnetillegg(): Int = this.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }

    fun beregnTotaltBeløp(): Int = beregnTotalOrdinærBeløp() + beregnTotalBarnetillegg()
}
