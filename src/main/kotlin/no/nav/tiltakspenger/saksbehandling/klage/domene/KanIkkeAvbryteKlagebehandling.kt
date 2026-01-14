package no.nav.tiltakspenger.saksbehandling.klage.domene

sealed interface KanIkkeAvbryteKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeAvbryteKlagebehandling
}
