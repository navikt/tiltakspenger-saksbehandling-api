package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

sealed interface KanIkkeSendeTilBeslutter {
    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder (gjelder både søknadsbehandling og revurdering) */
    data object InnvilgelsesperiodenOverlapperMedUtbetaltPeriode : KanIkkeSendeTilBeslutter
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeTilBeslutter
}
