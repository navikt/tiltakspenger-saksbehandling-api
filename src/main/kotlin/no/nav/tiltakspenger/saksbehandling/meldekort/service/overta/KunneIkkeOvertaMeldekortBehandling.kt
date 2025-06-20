package no.nav.tiltakspenger.saksbehandling.meldekort.service.overta

sealed interface KunneIkkeOvertaMeldekortBehandling {
    data object BehandlingenKanIkkeVæreGodkjentEllerIkkeRett : KunneIkkeOvertaMeldekortBehandling
    data object BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta : KunneIkkeOvertaMeldekortBehandling
    data object BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta : KunneIkkeOvertaMeldekortBehandling
    data object BehandlingenMåVæreUnderBeslutningForÅOverta : KunneIkkeOvertaMeldekortBehandling
    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSamme : KunneIkkeOvertaMeldekortBehandling
    data object KanIkkeOvertaAutomatiskBehandling : KunneIkkeOvertaMeldekortBehandling
}
