package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface KanIkkeTaBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeTaBehandling
}
