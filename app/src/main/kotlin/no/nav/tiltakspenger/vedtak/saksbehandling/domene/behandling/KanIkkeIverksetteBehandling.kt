package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

sealed interface KanIkkeIverksetteBehandling {
    data object MåVæreBeslutter : KanIkkeIverksetteBehandling
    data object KunneIkkeOppretteOppgave : KanIkkeIverksetteBehandling
}
