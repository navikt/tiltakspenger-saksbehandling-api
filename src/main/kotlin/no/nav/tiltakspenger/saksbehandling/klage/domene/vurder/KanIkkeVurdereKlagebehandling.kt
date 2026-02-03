package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeVurdereKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: KanIkkeOppdatereKlagebehandling) : KanIkkeVurdereKlagebehandling

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeVurdereKlagebehandling
}
