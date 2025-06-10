package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeSendeTilBeslutter {
    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder (gjelder både søknadsbehandling og revurdering) */
    data object InnvilgelsesperiodenOverlapperMedUtbetaltPeriode : KanIkkeSendeTilBeslutter
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
}
