package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class BeregningId(val value: String) {
    override fun toString() = value

    companion object {
        fun random(): BeregningId = BeregningId(UUID.randomUUID().toString())
    }
}

/** @param meldekortId Id for meldekort-behandlingen med utfylte dager for beregningen av denne perioden
 *  */
data class MeldeperiodeBeregning(
    val id: BeregningId,
    val meldekortId: MeldekortId,
    val kjedeId: MeldeperiodeKjedeId,
    val dager: NonEmptyList<MeldeperiodeBeregningDag>,
    val beregningKilde: BeregningKilde,
) {

    val fraOgMed: LocalDate get() = dager.first().dato
    val tilOgMed: LocalDate get() = dager.last().dato
    val periode: Periode get() = Periode(fraOgMed, tilOgMed)

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

    fun beregnTotalOrdinærBeløp(): Int = dager.sumOf { it.beregningsdag?.beløp ?: 0 }

    fun beregnTotalBarnetillegg(): Int = dager.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }

    fun beregnTotaltBeløp(): Int = beregnTotalOrdinærBeløp() + beregnTotalBarnetillegg()

    sealed interface BeregningKilde

    /** @param id Id for meldekort-behandlingen som utløste denne beregningen. Denne kan være ulik [meldekortId] for beregninger som er et resultat av en korrigering som påvirket en påfølgende meldeperiode.
     * */
    data class FraMeldekort(val id: MeldekortId) : BeregningKilde

    /** @param id Id for behandlingen/revurderingen som utløste denne beregningen.
     * */
    data class FraBehandling(val id: BehandlingId) : BeregningKilde
}
