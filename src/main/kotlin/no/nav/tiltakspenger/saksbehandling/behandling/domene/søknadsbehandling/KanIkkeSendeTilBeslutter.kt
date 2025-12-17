package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

sealed interface KanIkkeSendeTilBeslutter {
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeTilBeslutter
    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeSendeTilBeslutter
    data object ErPaVent : KanIkkeSendeTilBeslutter
}
