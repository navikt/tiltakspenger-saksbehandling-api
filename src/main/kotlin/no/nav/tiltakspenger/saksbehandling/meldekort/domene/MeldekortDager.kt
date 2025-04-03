package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate

data class MeldekortDager(
    val verdi: List<MeldekortDag>,
) : List<MeldekortDag> by verdi {

    val fraOgMed: LocalDate get() = this.first().dato
    val tilOgMed: LocalDate get() = this.last().dato
    val periode = Periode(fraOgMed, tilOgMed)

//     val maksDagerMedTiltakspengerForPeriode: Int,

    init {
        require(size == 14) { "Et meldekort må ha 14 dager, men hadde $size" }
        require(fraOgMed.dayOfWeek == DayOfWeek.MONDAY) { "Meldekortet må starte på en mandag" }
        require(tilOgMed.dayOfWeek == DayOfWeek.SUNDAY) { "Meldekortet må slutte på en søndag" }

        this.forEachIndexed { index, dag ->
            require(fraOgMed.plusDays(index.toLong()) == dag.dato) {
                "Datoene må være sammenhengende og sortert, men var ${this.map { it.dato }}"
            }
        }
    }
}

fun Meldeperiode.tilMeldekortDager() = MeldekortDager(
    this.girRett.entries.map { (dato, harRett) ->
        MeldekortDag(
            dato = dato,
            status = if (harRett) MeldekortDagStatus.IKKE_UTFYLT else MeldekortDagStatus.SPERRET,
        )
    },
)
