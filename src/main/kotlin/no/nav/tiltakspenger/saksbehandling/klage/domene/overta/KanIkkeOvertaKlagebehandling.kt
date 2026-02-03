package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeOvertaKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: KanIkkeOppdatereKlagebehandling) : KanIkkeOvertaKlagebehandling
    data object BrukTaKlagebehandlingIsteden : KanIkkeOvertaKlagebehandling
}
