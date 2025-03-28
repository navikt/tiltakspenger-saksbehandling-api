package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

sealed interface KanIkkeIverksetteBehandling {
    data object MåVæreBeslutter : KanIkkeIverksetteBehandling
    data object KunneIkkeOppretteOppgave : KanIkkeIverksetteBehandling
}
