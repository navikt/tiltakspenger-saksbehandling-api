package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

sealed interface KanIkkeSendeRammebehandlingTilBeslutter {
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeRammebehandlingTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeRammebehandlingTilBeslutter
    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeSendeRammebehandlingTilBeslutter
    data object ErPaVent : KanIkkeSendeRammebehandlingTilBeslutter
}
