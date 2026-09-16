package no.nav.tiltakspenger.saksbehandling.benk.domene

/**
 * Feil som gjør at benken ikke kan vises.
 * Radene bærer hvem saken gjelder, så uten en tilgangsvurdering skal ingenting ut av benken.
 */
sealed interface KunneIkkeHenteBenk {
    /**
     * Tilgangskontrollen feilet, og vi vet ikke hvilke rader saksbehandleren har lov til å se.
     * Servicen har allerede logget kallet, så kalleren trenger bare å svare med en serverfeil.
     */
    data object Tilgangskontroll : KunneIkkeHenteBenk
}
