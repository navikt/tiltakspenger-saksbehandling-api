package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

/**
 * Mulige grunner til at en meldekortbehandling ikke kan settes på vent.
 *
 * Se `Meldekortbehandling.kanSettePåVent`.
 */
sealed interface KanIkkeSetteMeldekortbehandlingPåVent {
    /** Behandlingen er allerede satt på vent. */
    data object BehandlingenErAlleredePåVent : KanIkkeSetteMeldekortbehandlingPåVent

    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeSetteMeldekortbehandlingPåVent

    /** Behandlingen kan kun settes på vent av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeSetteMeldekortbehandlingPåVent

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KanIkkeSetteMeldekortbehandlingPåVent

    /** Behandlingen kan kun settes på vent av beslutteren som er tildelt behandlingen. */
    data object MåVæreBeslutterForMeldekortet : KanIkkeSetteMeldekortbehandlingPåVent

    /** Behandlingen er i en status som ikke kan settes på vent. */
    data class UgyldigStatus(val status: MeldekortbehandlingStatus) : KanIkkeSetteMeldekortbehandlingPåVent
}
