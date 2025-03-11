package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

sealed interface KunneIkkeHenteSakForFnr {
    data object FantIkkeSakForFnr : KunneIkkeHenteSakForFnr
    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KunneIkkeHenteSakForFnr
}
