package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KanIkkeSendeMeldekortTilBeslutter {
    data class KanIkkeOppdatere(val underliggende: KanIkkeOppdatereMeldekort) : KanIkkeSendeMeldekortTilBeslutter
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortTilBeslutter
}
