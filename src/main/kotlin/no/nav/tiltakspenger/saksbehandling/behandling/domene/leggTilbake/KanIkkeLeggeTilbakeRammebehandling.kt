package no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.KanIkkeLeggeTilbakeKlagebehandling

/**
 * Mulige grunner til at en rammebehandling ikke kan legges tilbake.
 *
 * Se `Rammebehandling.kanLeggeTilbake`.
 */
sealed interface KanIkkeLeggeTilbakeRammebehandling : Loggbar {
    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeLeggeTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler saksbehandlerrolle")
    }

    /** Behandlingen kan kun legges tilbake av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerForBehandlingen : KanIkkeLeggeTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke saksbehandleren som er tildelt behandlingen")
    }

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KanIkkeLeggeTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler beslutterrolle")
    }

    /** Behandlingen kan kun legges tilbake av beslutteren som er tildelt behandlingen. */
    data object MåVæreBeslutterForBehandlingen : KanIkkeLeggeTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke beslutteren som er tildelt behandlingen")
    }

    /** Behandlingen er i en status som ikke kan legges tilbake, typisk fordi den endret status etter at frontenden hentet den. */
    data class UgyldigStatus(val status: Rammebehandlingsstatus) : KanIkkeLeggeTilbakeRammebehandling {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }

    /** Klagebehandlingen knyttet til rammebehandlingen kunne ikke legges tilbake. */
    data class FeilVedKlagebehandling(val originalfeil: KanIkkeLeggeTilbakeKlagebehandling) : KanIkkeLeggeTilbakeRammebehandling {
        override val loggkontekst get() = Loggkontekst("kunne ikke legge tilbake tilknyttet klagebehandling: $originalfeil")
    }
}
