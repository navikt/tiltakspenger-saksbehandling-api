package no.nav.tiltakspenger.saksbehandling.klage.domene

sealed interface KanIkkeOppretteKlagebehandling {
    data object FantIkkeJournalpost : KanIkkeOppretteKlagebehandling
}
