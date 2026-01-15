package no.nav.tiltakspenger.saksbehandling.klage.domene.opprett

sealed interface KanIkkeOppretteKlagebehandling {
    data object FantIkkeJournalpost : KanIkkeOppretteKlagebehandling
}
