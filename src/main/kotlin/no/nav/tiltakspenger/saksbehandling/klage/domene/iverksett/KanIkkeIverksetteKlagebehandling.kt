package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

sealed interface KanIkkeIverksetteKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeIverksetteKlagebehandling
}
