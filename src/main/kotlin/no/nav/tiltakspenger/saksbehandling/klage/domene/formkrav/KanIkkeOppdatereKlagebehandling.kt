package no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav

sealed interface KanIkkeOppdatereKlagebehandling {
    data object FantIkkeJournalpost : KanIkkeOppdatereKlagebehandling

    /** Behandlingen er i en tilstand der den ikke kan oppdateres. */
    data object KanIkkeOppdateres : KanIkkeOppdatereKlagebehandling
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeOppdatereKlagebehandling

    data object KanIkkeEndreTilAvvisningNårTilknyttetRammebehandling : KanIkkeOppdatereKlagebehandling
}
