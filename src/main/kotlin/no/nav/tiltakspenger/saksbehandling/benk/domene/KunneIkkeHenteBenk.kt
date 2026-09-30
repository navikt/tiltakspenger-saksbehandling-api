package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.person.KunneIkkeHenteAdressebeskyttelseEllerSkjerming

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

    /**
     * Oppslaget mot PDL eller skjermingsregisteret feilet under filtreringen.
     * Feilen er ikke logget; ruten logger den én gang med konteksten [årsak] bærer.
     */
    data class AdressebeskyttelseOgSkjerming(
        val årsak: KunneIkkeHenteAdressebeskyttelseEllerSkjerming,
    ) : KunneIkkeHenteBenk,
        Loggbar by årsak
}
