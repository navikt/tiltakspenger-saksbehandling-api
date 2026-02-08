package no.nav.tiltakspenger.saksbehandling.behandling.domene.overta

sealed interface KunneIkkeOvertaBehandling {
    data object BehandlingenKanIkkeVæreVedtattEllerAvbrutt : KunneIkkeOvertaBehandling
    data object BehandlingenKanIkkeVæreUnderAutomatiskBehandling : KunneIkkeOvertaBehandling
    data object BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta : KunneIkkeOvertaBehandling
    data object BehandlingenMåVæreUnderBehandlingForÅOverta : KunneIkkeOvertaBehandling
    data object BehandlingenMåVæreUnderBeslutningForÅOverta : KunneIkkeOvertaBehandling
    data object BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta : KunneIkkeOvertaBehandling
    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSamme : KunneIkkeOvertaBehandling
    data object BehandlingenErUnderAktivBehandling : KunneIkkeOvertaBehandling
    data class KanIkkeOvertaKlagebehandling(val underliggende: no.nav.tiltakspenger.saksbehandling.klage.domene.overta.KanIkkeOvertaKlagebehandling) : KunneIkkeOvertaBehandling
}
