package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import no.nav.tiltakspenger.libs.common.BehandlingId

sealed interface KanIkkeAvbryteKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeAvbryteKlagebehandling
    data class KnyttetTilIkkeAvbruttRammebehandling(val rammebehandlingId: BehandlingId) : KanIkkeAvbryteKlagebehandling
}
