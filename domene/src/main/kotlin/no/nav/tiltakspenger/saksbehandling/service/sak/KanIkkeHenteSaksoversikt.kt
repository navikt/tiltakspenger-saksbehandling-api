package no.nav.tiltakspenger.saksbehandling.service.sak

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

sealed interface KanIkkeHenteSaksoversikt {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KanIkkeHenteSaksoversikt
}
