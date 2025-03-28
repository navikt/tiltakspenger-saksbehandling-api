package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface KanIkkeHenteBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeHenteBehandling
}
