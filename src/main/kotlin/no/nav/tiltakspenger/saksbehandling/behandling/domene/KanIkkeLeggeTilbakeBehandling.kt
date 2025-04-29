package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeLeggeTilbakeBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeLeggeTilbakeBehandling
    data object MåVæreSaksbehandlerEllerBeslutterForBehandlingen : KanIkkeLeggeTilbakeBehandling
}
