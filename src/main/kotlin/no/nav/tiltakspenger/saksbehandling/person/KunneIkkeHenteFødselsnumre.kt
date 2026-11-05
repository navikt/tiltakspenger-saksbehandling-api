package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/**
 * Oppslaget av personens nåværende og historiske fødselsnumre i PDL feilet.
 * Feilen bærer sin egen loggkontekst og logges én gang av kallstedet.
 */
data class KunneIkkeHenteFødselsnumre(
    override val loggkontekst: Loggkontekst,
    override val sikkerloggkontekst: Loggkontekst?,
) : Loggbar
