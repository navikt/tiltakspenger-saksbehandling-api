package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeGjenopptaKlagebehandling {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String?,
    ) : KanIkkeGjenopptaKlagebehandling

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeGjenopptaKlagebehandling
}
