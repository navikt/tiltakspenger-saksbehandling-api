package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Dette vil være saksbehandlers eller systemets meldekort, som er det som blir godkjent. Trenger ikke være 1-1 med brukers meldekort.
 * Skal ikke brukes for brukers meldekort; se [no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort].
 * Gjelder 14 dager, fra mandag til søndag.
 *
 * @param dager Liste med 14 dager, fra mandag til søndag. Må inneholde alle dagene eksakt én gang.
 * @param meldeperiode Meldeperioden dagene tilhører.
 */
data class UtfyltMeldeperiode(
    val dager: List<MeldekortDag>,
    val meldeperiode: Meldeperiode,
) : List<MeldekortDag> by dager {

    val maksAntallDagerForPeriode = meldeperiode.maksAntallDagerForMeldeperiode

    val fraOgMed: LocalDate get() = this.first().dato
    val tilOgMed: LocalDate get() = this.last().dato
    val periode = Periode(fraOgMed, tilOgMed)

    private val antallDagerMedDeltattEllerFravær: Int get() = this.count { it.harDeltattEllerFravær }

    init {
        require(size == 14) { "Et meldekort må være 14 dager, men var $size" }
        require(fraOgMed.dayOfWeek == DayOfWeek.MONDAY) { "Meldekortet må starte på en mandag" }
        require(tilOgMed.dayOfWeek == DayOfWeek.SUNDAY) { "Meldekortet må slutte på en søndag" }
        require(periode == meldeperiode.periode) {
            "MeldekortDager (periode=$periode) må være lik meldeperioden ${meldeperiode.periode}"
        }
        dager.zipWithNext { a, b ->
            require(a.dato.plusDays(1) == b.dato) {
                "Datoene må være i stigende rekkefølge (sammenhengende, sortert og uten duplikater)."
            }
        }
        require(maksAntallDagerForPeriode >= 0 && maksAntallDagerForPeriode <= 14) {
            "Maks antall dager for perioden må være mellom 0 og 14"
        }
        require(maksAntallDagerForPeriode >= antallDagerMedDeltattEllerFravær) {
            "For mange dager utfylt - $antallDagerMedDeltattEllerFravær var utfylt, maks antall for perioden er $maksAntallDagerForPeriode (meldeperiode id ${meldeperiode.id})"
        }
        meldeperiode.girRett.toList().zip(dager) { (dato, harRett), dag ->
            require(dato == dag.dato) {
                "Meldeperiodedatoene må stemme overens med dagene."
            }
            if (harRett && dag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                throw IllegalArgumentException("Kan ikke endre dag til IKKE_RETT_TIL_TILTAKSPENGER. Meldeperiode (id ${meldeperiode.id}): ${meldeperiode.girRett}. Innsendte dager: $dager")
            }
            if (!harRett && dag.status != MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                throw IllegalArgumentException("Kan ikke endre dag fra IKKE_RETT_TIL_TILTAKSPENGER. Meldeperiode (id ${meldeperiode.id}): ${meldeperiode.girRett}. Innsendte dager: $dager")
            }
        }
    }
}

fun Meldeperiode.tilUtfyltMeldeperiode(): UtfyltMeldeperiode {
    return UtfyltMeldeperiode(
        periode.datoer.map { dato ->
            MeldekortDag(
                dato = dato,
                status = if (girRett[dato] == true) MeldekortDagStatus.IKKE_BESVART else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
            )
        }.toList(),
        this,
    )
}
