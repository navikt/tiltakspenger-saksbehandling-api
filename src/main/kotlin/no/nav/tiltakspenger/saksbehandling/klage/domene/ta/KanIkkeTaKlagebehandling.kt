package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

sealed interface KanIkkeTaKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: Klagebehandling.KanIkkeOppdateres) : KanIkkeTaKlagebehandling
}
