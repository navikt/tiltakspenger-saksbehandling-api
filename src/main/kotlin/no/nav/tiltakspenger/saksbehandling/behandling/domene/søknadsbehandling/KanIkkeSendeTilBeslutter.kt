package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

sealed interface KanIkkeSendeTilBeslutter {
    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder (gjelder søknadsbehandling) */
    data object InnvilgelsesperiodenOverlapperMedUtbetaltPeriode : KanIkkeSendeTilBeslutter

    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder som fører til tilbakekreving (gjelder revurdering) */
    data object StøtterIkkeTilbakekreving : KanIkkeSendeTilBeslutter
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeTilBeslutter
}
