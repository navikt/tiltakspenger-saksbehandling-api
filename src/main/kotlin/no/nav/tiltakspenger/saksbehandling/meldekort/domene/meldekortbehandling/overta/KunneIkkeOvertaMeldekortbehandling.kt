package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta

sealed interface KunneIkkeOvertaMeldekortbehandling {
    data object BehandlingenKanIkkeVæreGodkjentEllerIkkeRett : KunneIkkeOvertaMeldekortbehandling
    data object BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta : KunneIkkeOvertaMeldekortbehandling
    data object BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta : KunneIkkeOvertaMeldekortbehandling
    data object BehandlingenMåVæreUnderBeslutningForÅOverta : KunneIkkeOvertaMeldekortbehandling
    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSamme : KunneIkkeOvertaMeldekortbehandling
    data object KanIkkeOvertaAutomatiskBehandling : KunneIkkeOvertaMeldekortbehandling
}
