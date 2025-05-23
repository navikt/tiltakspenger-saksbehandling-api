package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KanIkkeOppdatereMeldekort {
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeOppdatereMeldekort
}
