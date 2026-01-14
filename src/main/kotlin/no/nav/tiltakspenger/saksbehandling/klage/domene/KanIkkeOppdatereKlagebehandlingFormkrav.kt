package no.nav.tiltakspenger.saksbehandling.klage.domene

sealed interface KanIkkeOppdatereKlagebehandlingFormkrav {
    data object FantIkkeJournalpost : KanIkkeOppdatereKlagebehandlingFormkrav
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeOppdatereKlagebehandlingFormkrav
}
