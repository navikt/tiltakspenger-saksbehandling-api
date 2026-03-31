package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett

sealed interface KanIkkeIverksetteMeldekortbehandling {
    data object SaksbehandlerOgBeslutterKanIkkeVæreLik : KanIkkeIverksetteMeldekortbehandling
    data object BehandlingenErIkkeUnderBeslutning : KanIkkeIverksetteMeldekortbehandling
    data object MåVæreBeslutterForMeldekortet : KanIkkeIverksetteMeldekortbehandling
}
