package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

sealed interface KanIkkeTaBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeTaBehandling
}
