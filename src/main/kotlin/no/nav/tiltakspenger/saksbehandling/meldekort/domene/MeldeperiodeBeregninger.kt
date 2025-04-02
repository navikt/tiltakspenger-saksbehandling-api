package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperiodeBeregninger(
    val verdi: List<MeldeperiodeBeregning>,
) : List<MeldeperiodeBeregning> by verdi {

    init {
        require(true)
    }

    private val beregningerSortert by lazy { this.sortedBy { it.beregnet } }

    val beregningerPerKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> by lazy {
        beregningerSortert.groupBy { it.kjedeId }
    }

    val beregningerPerMeldekort: Map<MeldekortId, List<MeldeperiodeBeregning>> by lazy {
        beregningerSortert.groupBy { it.meldekortId }
    }

    companion object {
        fun empty() = MeldeperiodeBeregninger(emptyList())
    }
}

data class MeldeperiodeBeregning(
    /** Id for meldekortbehandlingen som denne perioden er beregnet ut fra */
    val meldekortId: MeldekortId,
    val kjedeId: MeldeperiodeKjedeId,
    val sakId: SakId,
    val beregnet: LocalDateTime,
    val dager: NonEmptyList<MeldeperiodeBeregningDag.Utfylt>,
) {
    val fraOgMed: LocalDate get() = dager.first().dato
    val tilOgMed: LocalDate get() = dager.last().dato

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
}
