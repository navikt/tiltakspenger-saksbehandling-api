package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta

sealed interface KunneIkkeOvertaBehandling {
    data object BehandlingenKanIkkeVæreVedtattEllerAvbrutt : KunneIkkeOvertaBehandling
    data object BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta : KunneIkkeOvertaBehandling
    data object BehandlingenMåVæreUnderBehandlingForÅOverta : KunneIkkeOvertaBehandling
    data object BehandlingenMåVæreUnderBeslutningForÅOverta : KunneIkkeOvertaBehandling
    data object BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta : KunneIkkeOvertaBehandling
    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSamme : KunneIkkeOvertaBehandling
}
