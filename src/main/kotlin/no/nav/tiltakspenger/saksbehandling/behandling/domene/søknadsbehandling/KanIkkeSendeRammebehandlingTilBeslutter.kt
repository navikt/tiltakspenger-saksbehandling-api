package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeSendeRammebehandlingTilBeslutter {
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeRammebehandlingTilBeslutter
    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeRammebehandlingTilBeslutter
    data object ErPaVent : KanIkkeSendeRammebehandlingTilBeslutter
    data class UtbetalingFeil(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeSendeRammebehandlingTilBeslutter
    data class SimuleringFeil(val underliggende: KunneIkkeSimulere) : KanIkkeSendeRammebehandlingTilBeslutter
}
