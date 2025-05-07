package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate

/** @param beregningMeldekortId Id for meldekort-behandlingen som utløste denne beregningen. Denne kan være ulik [dagerMeldekortId] for beregninger som er et resultat av en korrigering som påvirket en påfølgende meldeperiode.
 *  @param dagerMeldekortId Id for meldekort-behandlingen med utfylte dager for beregningen av denne perioden
 *  */
data class MeldeperiodeBeregning(
    val beregningMeldekortId: MeldekortId,
    val dagerMeldekortId: MeldekortId,
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
    }

    fun beregnTotalOrdinærBeløp(): Int = this.sumOf { it.beregningsdag?.beløp ?: 0 }

    fun beregnTotalBarnetillegg(): Int = this.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }

    fun beregnTotaltBeløp(): Int = beregnTotalOrdinærBeløp() + beregnTotalBarnetillegg()
}
