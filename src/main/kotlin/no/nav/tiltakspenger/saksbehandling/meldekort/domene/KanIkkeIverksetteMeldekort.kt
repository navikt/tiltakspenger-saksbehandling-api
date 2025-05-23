package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KanIkkeIverksetteMeldekort {
    data object SaksbehandlerOgBeslutterKanIkkeVæreLik : KanIkkeIverksetteMeldekort
    data object BehandlingenErIkkeUnderBeslutning : KanIkkeIverksetteMeldekort
    data object MåVæreBeslutterForMeldekortet : KanIkkeIverksetteMeldekort
}
