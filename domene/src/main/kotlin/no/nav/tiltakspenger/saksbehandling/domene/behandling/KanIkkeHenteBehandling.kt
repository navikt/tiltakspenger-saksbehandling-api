package no.nav.tiltakspenger.saksbehandling.domene.behandling

sealed interface KanIkkeHenteBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeHenteBehandling
}
