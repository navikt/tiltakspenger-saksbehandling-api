package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling as MeldekortOvertaFeil

sealed interface KanIkkeOvertaKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: KanIkkeOppdatereKlagebehandling) : KanIkkeOvertaKlagebehandling

    data object BrukTaKlagebehandlingIsteden : KanIkkeOvertaKlagebehandling

    data class KunneIkkeOvertaRammebehandling(val underliggende: no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.KunneIkkeOvertaBehandling) : KanIkkeOvertaKlagebehandling
    data class KunneIkkeOvertaMeldekortbehandling(val underliggende: MeldekortOvertaFeil) : KanIkkeOvertaKlagebehandling
}
