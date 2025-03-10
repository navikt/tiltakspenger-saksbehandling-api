package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

sealed interface KanIkkeTaBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeTaBehandling
}
