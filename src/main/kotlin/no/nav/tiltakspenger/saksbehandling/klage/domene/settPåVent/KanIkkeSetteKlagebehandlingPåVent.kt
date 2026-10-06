package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeSetteKlagebehandlingPåVent {
    data object BehandlingenErIkkeLengerUnderBeslutning : KanIkkeSetteKlagebehandlingPåVent

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String?,
    ) : KanIkkeSetteKlagebehandlingPåVent

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeSetteKlagebehandlingPåVent
}
