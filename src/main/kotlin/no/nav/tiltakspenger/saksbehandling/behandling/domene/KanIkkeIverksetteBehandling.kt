package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeIverksetteBehandling {
    data object MåVæreBeslutter : KanIkkeIverksetteBehandling
    data object KunneIkkeOppretteOppgave : KanIkkeIverksetteBehandling
    data class BehandlingenEiesAvAnnenBeslutter(val eiesAvBeslutter: String?) : KanIkkeIverksetteBehandling
}
