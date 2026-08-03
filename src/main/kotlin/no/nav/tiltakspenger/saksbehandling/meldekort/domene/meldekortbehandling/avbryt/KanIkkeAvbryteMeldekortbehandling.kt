package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

sealed interface KanIkkeAvbryteMeldekortbehandling {
    /** Behandlingen kan kun avbrytes av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeAvbryteMeldekortbehandling

    /** Behandlingen kan kun avbrytes av beslutteren som er tildelt behandlingen. */
    data object MåVæreBeslutterForMeldekortet : KanIkkeAvbryteMeldekortbehandling

    /** Behandlingen er i en status som ikke kan avbrytes. */
    data class UgyldigStatus(val status: MeldekortbehandlingStatus) : KanIkkeAvbryteMeldekortbehandling
}
