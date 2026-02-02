package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

sealed interface KanIkkeOvertaKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: Klagebehandling.KanIkkeOppdateres) : KanIkkeOvertaKlagebehandling
}
