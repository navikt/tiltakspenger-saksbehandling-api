package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KanIkkeLeggeTilbakeMeldekortBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeLeggeTilbakeMeldekortBehandling
    data object MåVæreSaksbehandlerEllerBeslutterForBehandlingen : KanIkkeLeggeTilbakeMeldekortBehandling
}
