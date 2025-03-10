package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface KanIkkeIverksetteBehandling {
    data object MåVæreBeslutter : KanIkkeIverksetteBehandling
    data object KunneIkkeOppretteOppgave : KanIkkeIverksetteBehandling
}
