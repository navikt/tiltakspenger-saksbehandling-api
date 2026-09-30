package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/**
 * Oppslaget av adressebeskyttelse eller skjerming feilet.
 * Feilen bærer sin egen loggkontekst og logges én gang av route-laget.
 */
sealed interface KunneIkkeHenteAdressebeskyttelseEllerSkjerming : Loggbar {
    data class FeilVedKallMotPdl(
        override val loggkontekst: Loggkontekst,
        override val sikkerloggkontekst: Loggkontekst?,
    ) : KunneIkkeHenteAdressebeskyttelseEllerSkjerming

    data class FeilVedKallMotSkjerming(
        override val loggkontekst: Loggkontekst,
        override val sikkerloggkontekst: Loggkontekst?,
    ) : KunneIkkeHenteAdressebeskyttelseEllerSkjerming
}
