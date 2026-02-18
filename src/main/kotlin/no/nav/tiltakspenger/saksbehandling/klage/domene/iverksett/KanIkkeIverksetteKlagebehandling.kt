package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

sealed interface KanIkkeIverksetteKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeIverksetteKlagebehandling
    data class MåHaStatusUnderBehandling(val actualStatus: String) : KanIkkeIverksetteKlagebehandling
    data class FeilResultat(val forventetResultat: String, val faktiskResultat: String?) : KanIkkeIverksetteKlagebehandling
    data class FeilInngang(val forventetInngang: String, val faktiskInngang: String) : KanIkkeIverksetteKlagebehandling
    data object ManglerBrevtekst : KanIkkeIverksetteKlagebehandling
}
