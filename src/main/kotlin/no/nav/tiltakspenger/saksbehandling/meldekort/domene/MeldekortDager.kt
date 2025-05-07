package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Gjelder 14 dager, fra mandag til søndag.
 *
 * @param verdi Liste med 14 dager, fra mandag til søndag. Må inneholde alle dagene eksakt én gang.
 * @param maksAntallDagerForPeriode Maks antall dager som kan være utfylt i perioden. Bestemmes i behandlingen. Mellom 0 og 14.
 */
data class MeldekortDager(
    val verdi: List<MeldekortDag>,
    val maksAntallDagerForPeriode: Int,
) : List<MeldekortDag> by verdi {

    val fraOgMed: LocalDate get() = this.first().dato
    val tilOgMed: LocalDate get() = this.last().dato
    val periode = Periode(fraOgMed, tilOgMed)

    private val antallDagerMedDeltattEllerFravær: Int get() = this.count { it.harDeltattEllerFravær }

    init {
        require(size == 14) { "Et meldekort må være 14 dager, men var $size" }
        require(fraOgMed.dayOfWeek == DayOfWeek.MONDAY) { "Meldekortet må starte på en mandag" }
        require(tilOgMed.dayOfWeek == DayOfWeek.SUNDAY) { "Meldekortet må slutte på en søndag" }
        verdi.zipWithNext { a, b ->
            require(a.dato.plusDays(1) == b.dato) {
                "Datoene må være i stigende rekkefølge (sammenhengende, sortert og uten duplikater)."
            }
        }
        require(maksAntallDagerForPeriode >= 0 && maksAntallDagerForPeriode <= 14) {
            "Maks antall dager for perioden må være mellom 0 og 14"
        }
        require(maksAntallDagerForPeriode >= antallDagerMedDeltattEllerFravær) {
            "For mange dager utfylt - $antallDagerMedDeltattEllerFravær var utfylt, maks antall for perioden er $maksAntallDagerForPeriode"
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
