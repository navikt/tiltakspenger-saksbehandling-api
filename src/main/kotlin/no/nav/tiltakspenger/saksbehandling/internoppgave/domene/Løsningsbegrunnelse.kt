package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.libs.common.NonBlankString

/** Saksbehandlerens begrunnelse for valgt løsning, enten en forhåndsdefinert årsak eller fritekst. */
sealed interface Løsningsbegrunnelse {
    data class Forhåndsdefinert(val årsak: Årsak) : Løsningsbegrunnelse

    data class Fritekst(val tekst: NonBlankString) : Løsningsbegrunnelse {
        override fun toString(): String = "Fritekst(*****)"
    }

    /**
     * Foreløpige plassholdere.
     * Endelige årsaker må avklares før oppgavesystemet tas i bruk.
     */
    enum class Årsak {
        ENDRINGEN_ER_ALLEREDE_HÅNDTERT,
        ENDRINGEN_PÅVIRKER_IKKE_RETTEN,
    }
}
