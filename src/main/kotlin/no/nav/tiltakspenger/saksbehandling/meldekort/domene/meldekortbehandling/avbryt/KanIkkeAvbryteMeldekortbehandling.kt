package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt

sealed interface KanIkkeAvbryteMeldekortbehandling {
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeAvbryteMeldekortbehandling
    data object MåVæreUnderBehandling : KanIkkeAvbryteMeldekortbehandling
}
