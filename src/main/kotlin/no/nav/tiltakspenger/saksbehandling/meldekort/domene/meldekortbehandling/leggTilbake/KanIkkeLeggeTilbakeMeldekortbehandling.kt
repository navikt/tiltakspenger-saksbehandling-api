package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

/**
 * Mulige grunner til at en meldekortbehandling ikke kan legges tilbake.
 *
 * Se `Meldekortbehandling.kanLeggeTilbake`.
 */
sealed interface KanIkkeLeggeTilbakeMeldekortbehandling {
    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeLeggeTilbakeMeldekortbehandling

    /** Behandlingen kan kun legges tilbake av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeLeggeTilbakeMeldekortbehandling

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KanIkkeLeggeTilbakeMeldekortbehandling

    /** Behandlingen kan kun legges tilbake av beslutteren som er tildelt behandlingen. */
    data object MåVæreBeslutterForMeldekortet : KanIkkeLeggeTilbakeMeldekortbehandling

    /** Behandlingen er i en status som ikke kan legges tilbake. */
    data class UgyldigStatus(val status: MeldekortbehandlingStatus) : KanIkkeLeggeTilbakeMeldekortbehandling
}
