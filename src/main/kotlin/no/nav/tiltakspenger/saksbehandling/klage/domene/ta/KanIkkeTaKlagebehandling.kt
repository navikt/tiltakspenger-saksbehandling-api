package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeTaKlagebehandling {
    data class KanIkkeOppdateres(val underliggende: KanIkkeOppdatereKlagebehandling) : KanIkkeTaKlagebehandling
    data object BrukOvertaIsteden : KanIkkeTaKlagebehandling
}
