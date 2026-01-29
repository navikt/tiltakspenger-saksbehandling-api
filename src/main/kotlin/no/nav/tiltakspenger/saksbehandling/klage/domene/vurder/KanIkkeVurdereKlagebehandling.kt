package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

sealed interface KanIkkeVurdereKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: Klagebehandling.KanIkkeOppdateres) : KanIkkeVurdereKlagebehandling

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeVurdereKlagebehandling
}
