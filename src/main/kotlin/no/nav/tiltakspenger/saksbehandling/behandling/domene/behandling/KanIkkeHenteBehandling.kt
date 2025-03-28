package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

sealed interface KanIkkeHenteBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeHenteBehandling
}
