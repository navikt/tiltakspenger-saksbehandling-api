package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

sealed interface KunneIkkeHenteSakForSaksnummer {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KunneIkkeHenteSakForSaksnummer
}
