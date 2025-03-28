package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeTaBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeTaBehandling
}
