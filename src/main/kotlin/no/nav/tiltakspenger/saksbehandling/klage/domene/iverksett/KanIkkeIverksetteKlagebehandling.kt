package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

sealed interface KanIkkeIverksetteKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeIverksetteKlagebehandling
    data class MåHaStatusUnderBehandling(val actualStatus: String) : KanIkkeIverksetteKlagebehandling
    data class MåHaResultatAvvisning(val actualResultat: String) : KanIkkeIverksetteKlagebehandling
    data class AndreGrunner(val årsak: List<String>) : KanIkkeIverksetteKlagebehandling
}
