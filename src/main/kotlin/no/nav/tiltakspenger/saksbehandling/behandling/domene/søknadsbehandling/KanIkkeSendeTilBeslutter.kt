package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

sealed interface KanIkkeSendeTilBeslutter {
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeTilBeslutter
    data object MåHaSimuleringAvUtbetaling : KanIkkeSendeTilBeslutter
    data object StøtterIkkeFeilutbetaling : KanIkkeSendeTilBeslutter
    data object StøtterIkkeUtbetalingJustering : KanIkkeSendeTilBeslutter
}
