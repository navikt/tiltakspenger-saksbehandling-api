package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

sealed interface KanIkkeHenteBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeHenteBehandling
}
