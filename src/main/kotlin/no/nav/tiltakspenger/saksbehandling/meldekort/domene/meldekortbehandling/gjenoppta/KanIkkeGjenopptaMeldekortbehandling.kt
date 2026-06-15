package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

/**
 * Mulige grunner til at en meldekortbehandling ikke kan gjenopptas.
 *
 * Se `Meldekortbehandling.kanGjenoppta`.
 */
sealed interface KanIkkeGjenopptaMeldekortbehandling {
    /** Behandlingen er ikke satt på vent, og kan dermed ikke gjenopptas. */
    data object BehandlingenErIkkePåVent : KanIkkeGjenopptaMeldekortbehandling

    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeGjenopptaMeldekortbehandling

    /** Behandlingen kan kun gjenopptas av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerSomEierBehandlingen : KanIkkeGjenopptaMeldekortbehandling

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KanIkkeGjenopptaMeldekortbehandling

    /** Beslutteren kan ikke være den samme som saksbehandleren på behandlingen. */
    data object BeslutterKanIkkeVæreSammeSomSaksbehandler : KanIkkeGjenopptaMeldekortbehandling

    /** Behandlingen er i en status som ikke kan gjenopptas. */
    data class UgyldigStatus(val status: MeldekortbehandlingStatus) : KanIkkeGjenopptaMeldekortbehandling
}
