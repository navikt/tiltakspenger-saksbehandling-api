package no.nav.tiltakspenger.saksbehandling.meldekort.domene

sealed interface KanIkkeAvbryteMeldekortBehandling {
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeAvbryteMeldekortBehandling
    data object MåVæreUnderBehandling : KanIkkeAvbryteMeldekortBehandling
}
