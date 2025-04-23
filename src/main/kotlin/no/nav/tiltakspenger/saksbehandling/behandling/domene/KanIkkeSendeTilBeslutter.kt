package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeSendeTilBeslutter {
    data object MåVæreSaksbehandler : KanIkkeSendeTilBeslutter
    data object PeriodenOverlapperEllerTilstøterMedAnnenBehandling : KanIkkeSendeTilBeslutter
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
}
