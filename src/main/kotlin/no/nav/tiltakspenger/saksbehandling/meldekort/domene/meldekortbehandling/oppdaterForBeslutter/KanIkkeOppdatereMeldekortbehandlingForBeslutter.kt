package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter

sealed interface KanIkkeOppdatereMeldekortbehandlingForBeslutter {
    data object BehandlingenErIkkeUnderBeslutning : KanIkkeOppdatereMeldekortbehandlingForBeslutter

    data object MåVæreBeslutterForMeldekortet : KanIkkeOppdatereMeldekortbehandlingForBeslutter

    data object BehandlingenErSattPåVent : KanIkkeOppdatereMeldekortbehandlingForBeslutter

    data object MåHaBegrunnelseForÅJournalføreNotat : KanIkkeOppdatereMeldekortbehandlingForBeslutter

    data object BehandlingenErIkkeLengerUnderBeslutning : KanIkkeOppdatereMeldekortbehandlingForBeslutter
}
