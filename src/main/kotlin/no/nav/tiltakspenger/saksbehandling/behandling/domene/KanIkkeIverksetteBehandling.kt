package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeIverksetteBehandling {
    data class BehandlingenEiesAvAnnenBeslutter(val eiesAvBeslutter: String?) : KanIkkeIverksetteBehandling
    data class SimuleringFeil(val feil: KunneIkkeSimulere) : KanIkkeIverksetteBehandling
    data class UtbetalingFeil(val feil: KanIkkeIverksetteUtbetaling, val sak: Sak, val behandling: Rammebehandling) : KanIkkeIverksetteBehandling
}
