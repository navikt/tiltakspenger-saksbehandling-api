package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

sealed interface KanIkkeSendeTilBeslutter {
    /** Inntil videre støtter vi ikke vedtak som fører til tilbakekreving (gjelder både søknadsbehandling og revurdering) */
    data object StøtterIkkeTilbakekreving : KanIkkeSendeTilBeslutter
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeTilBeslutter
}
