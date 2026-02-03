package no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeLeggeTilbakeKlagebehandling {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String?,
    ) : KanIkkeLeggeTilbakeKlagebehandling

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeLeggeTilbakeKlagebehandling
}
