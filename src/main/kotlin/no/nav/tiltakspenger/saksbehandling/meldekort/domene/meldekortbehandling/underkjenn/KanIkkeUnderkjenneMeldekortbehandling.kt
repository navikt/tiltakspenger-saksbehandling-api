package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn

sealed interface KanIkkeUnderkjenneMeldekortbehandling {
    data object BegrunnelseMåVæreUtfylt : KanIkkeUnderkjenneMeldekortbehandling
    data object BehandlingenErIkkeUnderBeslutning : KanIkkeUnderkjenneMeldekortbehandling
    data object SaksbehandlerKanIkkeUnderkjenneSinEgenBehandling : KanIkkeUnderkjenneMeldekortbehandling
    data object BehandlingenErAlleredeBesluttet : KanIkkeUnderkjenneMeldekortbehandling
    data object MåVæreBeslutterForMeldekortet : KanIkkeUnderkjenneMeldekortbehandling
}
