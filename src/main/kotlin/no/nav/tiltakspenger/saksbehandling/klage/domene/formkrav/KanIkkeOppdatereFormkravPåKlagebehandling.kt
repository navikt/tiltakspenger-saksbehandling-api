package no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav

sealed interface KanIkkeOppdatereFormkravPåKlagebehandling {
    data object FantIkkeJournalpost : KanIkkeOppdatereFormkravPåKlagebehandling

    /** Behandlingen er i en tilstand der den ikke kan oppdateres. */
    data object KanIkkeOppdateres : KanIkkeOppdatereFormkravPåKlagebehandling

    data class SaksbehandlerMismatch(val forventetSaksbehandler: String, val faktiskSaksbehandler: String) : KanIkkeOppdatereFormkravPåKlagebehandling

    data object KanIkkeEndreTilAvvisningNårTilknyttetBehandling : KanIkkeOppdatereFormkravPåKlagebehandling
}
