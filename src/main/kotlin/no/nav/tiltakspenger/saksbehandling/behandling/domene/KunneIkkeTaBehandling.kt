package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.KanIkkeTaKlagebehandling

sealed interface KunneIkkeTaBehandling {
    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSammePåBehandling : KunneIkkeTaBehandling

    data object BehandlingenHarEksisterendeSaksbehandler : KunneIkkeTaBehandling

    data object BehandlingenHarEksisterendeBeslutter : KunneIkkeTaBehandling

    data object BehandlingenErIEnTilstandSomIkkeTillaterÅTaBehandling : KunneIkkeTaBehandling

    data class FeilVedKlagebehandling(val originalfeil: KanIkkeTaKlagebehandling) : KunneIkkeTaBehandling
}
