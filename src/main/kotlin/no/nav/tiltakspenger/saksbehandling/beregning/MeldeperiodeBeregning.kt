package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.sumOf

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

    val ordinærBeløp: Int get() = dager.sumOf { it.beregningsdag?.beløp ?: 0 }
    val barnetilleggBeløp: Int get() = dager.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }
    val totalBeløp: Int get() = ordinærBeløp + barnetilleggBeløp

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
}

fun List<MeldeperiodeBeregning>.beregnOrdinærBeløp(): Int = this.sumOf { it.ordinærBeløp }
fun List<MeldeperiodeBeregning>.beregnBarnetilleggBeløp(): Int = this.sumOf { it.barnetilleggBeløp }
fun List<MeldeperiodeBeregning>.beregnTotalBeløp(): Int = this.sumOf { it.totalBeløp }
