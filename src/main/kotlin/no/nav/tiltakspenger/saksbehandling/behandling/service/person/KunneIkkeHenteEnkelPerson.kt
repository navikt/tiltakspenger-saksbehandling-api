package no.nav.tiltakspenger.saksbehandling.behandling.service.person

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

sealed interface KunneIkkeHenteEnkelPerson {
    data object FeilVedKallMotPdl : KunneIkkeHenteEnkelPerson
    data object FantIkkeSakId : KunneIkkeHenteEnkelPerson
    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KunneIkkeHenteEnkelPerson
}
