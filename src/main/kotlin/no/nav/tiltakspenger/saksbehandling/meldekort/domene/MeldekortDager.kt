package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate

data class MeldekortDager(
    val verdi: List<MeldekortDag>,
    val maksAntallDagerForPeriode: Int,
) : List<MeldekortDag> by verdi {

    val fraOgMed: LocalDate get() = this.first().dato
    val tilOgMed: LocalDate get() = this.last().dato
    val periode = Periode(fraOgMed, tilOgMed)

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

    companion object {
        /**
         * @param meldeperiode Perioden meldekortet skal gjelde for. Må være 14 dager, starte på en mandag og slutte på en søndag.
         * @return Meldekortdager for meldeperioden
         * @throws IllegalStateException Dersom alle dagene i en meldekortperiode er SPERRET er den per definisjon utfylt. Dette har vi ikke støtte for i MVP.
         */
        fun fraMeldeperiode(
            meldeperiode: Meldeperiode,
        ): MeldekortDager {
            val dager = meldeperiode.girRett.entries.map { (dato, girRett) ->
                MeldekortDag(
                    dato = dato,
                    status = if (girRett) MeldekortDagStatus.IKKE_UTFYLT else MeldekortDagStatus.SPERRET,
                )
            }

            return if (dager.any { it.status == MeldekortDagStatus.IKKE_UTFYLT }) {
                MeldekortDager(dager, meldeperiode.antallDagerForPeriode)
            } else {
                throw IllegalStateException("Alle dagene i en meldekortperiode er SPERRET. Dette har vi ikke støtte for i MVP.")
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
    this.antallDagerForPeriode,
)
