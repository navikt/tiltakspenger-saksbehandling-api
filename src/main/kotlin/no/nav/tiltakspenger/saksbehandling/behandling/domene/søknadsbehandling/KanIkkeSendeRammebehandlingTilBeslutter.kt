package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeSendeRammebehandlingTilBeslutter {
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeRammebehandlingTilBeslutter

    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeRammebehandlingTilBeslutter
    data object ErPaVent : KanIkkeSendeRammebehandlingTilBeslutter
    data class UtbetalingFeil(val feil: KanIkkeIverksetteUtbetaling, val sak: Sak, val behandling: Rammebehandling) : KanIkkeSendeRammebehandlingTilBeslutter

    data class SimuleringFeil(val feil: KunneIkkeSimulere) : KanIkkeSendeRammebehandlingTilBeslutter
}
