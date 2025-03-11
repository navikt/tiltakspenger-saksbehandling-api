package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface KanIkkeSendeTilBeslutter {
    data object MåVæreSaksbehandler : KanIkkeSendeTilBeslutter
}
