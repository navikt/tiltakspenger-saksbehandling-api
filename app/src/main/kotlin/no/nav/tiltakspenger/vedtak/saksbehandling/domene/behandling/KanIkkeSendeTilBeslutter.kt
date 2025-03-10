package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

sealed interface KanIkkeSendeTilBeslutter {
    data object MåVæreSaksbehandler : KanIkkeSendeTilBeslutter
}
