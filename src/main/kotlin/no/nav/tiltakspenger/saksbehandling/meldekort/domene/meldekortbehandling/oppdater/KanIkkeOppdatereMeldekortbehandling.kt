package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater

sealed interface KanIkkeOppdatereMeldekortbehandling {
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeOppdatereMeldekortbehandling
}
