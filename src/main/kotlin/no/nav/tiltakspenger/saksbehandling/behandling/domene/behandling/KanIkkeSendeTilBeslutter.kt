package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

sealed interface KanIkkeSendeTilBeslutter {
    data object MåVæreSaksbehandler : KanIkkeSendeTilBeslutter
    data object PeriodenOverlapperEllerTilstøterMedAnnenBehandling : KanIkkeSendeTilBeslutter
}
