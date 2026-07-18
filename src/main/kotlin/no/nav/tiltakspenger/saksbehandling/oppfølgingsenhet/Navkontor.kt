package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

/**
 * Også kallt kontornummer/enhetsnummer/oppfølgingsenhet.
 * Inngang befolkning: https://www.nav.no/sok-nav-kontor eksempel https://www.nav.no/kontor/nav-asker
 * Se også etterlatte sin take på det samme: https://github.com/navikt/pensjon-etterlatte-saksbehandling/blob/main/libs/saksbehandling-common/src/main/kotlin/Enhetsnummer.kt
 */
data class Navkontor(
    val kontornummer: String,
    val kontornavn: String?,
) {
    /**
     * Navkontor er stedslokaliserende persondata og skal ikke havne i vanlig logg.
     * Vi overstyrer derfor [toString] slik at verken [kontornummer] eller [kontornavn] eksponeres dersom et [Navkontor]-objekt ved et uhell blir logget (f.eks. via en data class som inneholder det).
     * Trenger man de faktiske verdiene må man hente dem eksplisitt og logge til sikkerlogg.
     */
    override fun toString(): String = "Navkontor(********)"

    /**
     * Full representasjon med de faktiske verdiene.
     * Skal KUN brukes når vi logger til sikkerlogg (eller annet sted hvor det er lov å eksponere stedslokaliserende persondata) - aldri i vanlig logg.
     */
    fun toStringForSikkerlogg(): String = "Navkontor(kontornummer=$kontornummer, kontornavn=$kontornavn)"
}
