package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson

sealed interface KunneIkkeHenteBarnFraFolkeregister {
    /**
     * Barnet finnes ikke, eller er ikke registrert som søkers barn.
     * Vi skiller ikke mellom disse.
     */
    data object IkkeBarnAvSøker : KunneIkkeHenteBarnFraFolkeregister

    data class FeilVedOppslag(val feil: KunneIkkeHenteEnkelPerson) : KunneIkkeHenteBarnFraFolkeregister
}
