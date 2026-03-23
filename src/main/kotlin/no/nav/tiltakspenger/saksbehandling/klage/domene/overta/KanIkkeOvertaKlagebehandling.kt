package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeOvertaKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: KanIkkeOppdatereKlagebehandling) : KanIkkeOvertaKlagebehandling
    data object BrukTaKlagebehandlingIsteden : KanIkkeOvertaKlagebehandling
    data class KunneIkkeOvertaRammebehandling(val underliggende: no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.KunneIkkeOvertaBehandling) : KanIkkeOvertaKlagebehandling
}
