package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KunneIkkeUnderkjenneMeldekortBehandling {
    data object BegrunnelseMåVæreUtfylt : KunneIkkeUnderkjenneMeldekortBehandling
    data object BehandlingenErIkkeKlarTilBeslutning : KunneIkkeUnderkjenneMeldekortBehandling
    data object SaksbehandlerKanIkkeUnderkjenneSinEgenBehandling : KunneIkkeUnderkjenneMeldekortBehandling
    data object BehandlingenErAlleredeBesluttet : KunneIkkeUnderkjenneMeldekortBehandling
}
