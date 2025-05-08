package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KanIkkeAvbryteMeldekortBehandling {
    data object MåVæreSaksbehandlerEllerBeslutter : KanIkkeAvbryteMeldekortBehandling
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeAvbryteMeldekortBehandling
    data object MåVæreUnderBehandling : KanIkkeAvbryteMeldekortBehandling
    data object MåVæreOpprettetAvSaksbehandler : KanIkkeAvbryteMeldekortBehandling
}
