package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeHenteBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeHenteBehandling
}
