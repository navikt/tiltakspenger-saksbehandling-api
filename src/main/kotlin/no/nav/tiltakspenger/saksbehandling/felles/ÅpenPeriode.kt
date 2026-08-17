package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/**
 * En periode der fraOgMed og/eller tilOgMed kan være ukjent (null).
 * null betyr at datoen er ukjent, ikke at perioden er uendelig.
 * Overlappsspørsmål svarer derfor tredelt: true/false der svaret er sikkert, og null der en ukjent dato gjør at begge utfall er mulige.
 */
data class ÅpenPeriode(
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
) {
    init {
        require(fraOgMed == null || tilOgMed == null || !fraOgMed.isAfter(tilOgMed)) {
            "fraOgMed ($fraOgMed) kan ikke være etter tilOgMed ($tilOgMed)"
        }
    }

    /**
     * Den lukkede perioden, eller null dersom [fraOgMed] eller [tilOgMed] mangler.
     */
    val periode: Periode? by lazy {
        if (fraOgMed != null && tilOgMed != null) Periode(fraOgMed, tilOgMed) else null
    }

    /**
     * @return true hvis vi med sikkerhet kan si at de overlapper, false hvis vi med sikkerhet vet at de ikke overlapper, og null dersom de kan overlappe.
     */
    fun overlapperMed(periode: Periode): Boolean? {
        this.periode?.let { return it.overlapperMed(periode) }

        // Hvis begge datoene mangler kan vi ikke si noe om overlapp og må dermed anta at de kan overlappe
        if (fraOgMed == null && tilOgMed == null) return null

        if (tilOgMed != null && fraOgMed == null) {
            if (periode.inneholder(tilOgMed)) return true
            if (tilOgMed.isBefore(periode.fraOgMed)) return false
        }

        if (fraOgMed != null && tilOgMed == null) {
            if (periode.inneholder(fraOgMed)) return true
            if (fraOgMed.isAfter(periode.tilOgMed)) return false
        }

        return null
    }

    /**
     * @return true hvis vi med sikkerhet kan si at de overlapper, false hvis vi med sikkerhet vet at de ikke overlapper, og null dersom de kan overlappe.
     */
    fun overlapperMed(other: ÅpenPeriode): Boolean? {
        val thisPeriode = this.periode
        val otherPeriode = other.periode
        return when {
            thisPeriode != null && otherPeriode != null -> thisPeriode.overlapperMed(otherPeriode)

            thisPeriode != null -> other.overlapperMed(thisPeriode)

            otherPeriode != null -> this.overlapperMed(otherPeriode)

            // Deler periodene en kjent dato overlapper de i hvert fall på den datoen, ellers kan vi ikke si noe sikkert
            else -> if (delerDato(other)) true else null
        }
    }

    private fun delerDato(other: ÅpenPeriode): Boolean {
        val andresDatoer = listOfNotNull(other.fraOgMed, other.tilOgMed)
        return listOfNotNull(fraOgMed, tilOgMed).any { it in andresDatoer }
    }
}
