package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeIverksetteBehandling {
    data class BehandlingenEiesAvAnnenBeslutter(val eiesAvBeslutter: String?) : KanIkkeIverksetteBehandling
    data object KanIkkeHaUtbetaling : KanIkkeIverksetteBehandling
    data class SimuleringFeilet(val underliggende: KunneIkkeSimulere) : KanIkkeIverksetteBehandling
    data object KunneIkkeHenteNavkontorForUtbetaling : KanIkkeIverksetteBehandling
    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeIverksetteBehandling
}
