package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeGjenopptaKlagebehandling {

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeGjenopptaKlagebehandling

    data object MåVæreSattPåVent : KanIkkeGjenopptaKlagebehandling
}
