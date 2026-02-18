package no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold

sealed interface KanIkkeOpprettholdeKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeOpprettholdeKlagebehandling
    data class MåHaStatusUnderBehandling(val actualStatus: String) : KanIkkeOpprettholdeKlagebehandling
    data class FeilResultat(val forventetResultat: String, val faktiskResultat: String?) : KanIkkeOpprettholdeKlagebehandling
    data object ManglerBrevtekst : KanIkkeOpprettholdeKlagebehandling
}
