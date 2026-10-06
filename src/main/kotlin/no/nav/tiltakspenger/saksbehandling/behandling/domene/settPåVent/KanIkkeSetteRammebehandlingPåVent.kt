package no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.KanIkkeSetteKlagebehandlingPåVent

/**
 * Mulige grunner til at en rammebehandling ikke kan settes på vent.
 *
 * Forhåndssjekkene gjøres i `Rammebehandling.kanSettePåVent`.
 * Feil kan også oppstå ved oppdatering av tilknyttet klagebehandling eller ved lagring etter en samtidig endring.
 */
sealed interface KanIkkeSetteRammebehandlingPåVent : Loggbar {
    /** Behandlingen er allerede satt på vent. */
    data object BehandlingenErAlleredePåVent : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst = Loggkontekst("behandlingen er allerede satt på vent")
    }

    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler saksbehandlerrolle")
    }

    /** Behandlingen kan kun settes på vent av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerForBehandlingen : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke saksbehandleren som er tildelt behandlingen")
    }

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler beslutterrolle")
    }

    /** Behandlingen kan kun settes på vent av beslutteren som er tildelt behandlingen. */
    data object MåVæreBeslutterForBehandlingen : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke beslutteren som er tildelt behandlingen")
    }

    /** Behandlingen er i en status som ikke kan settes på vent. */
    data class UgyldigStatus(val status: Rammebehandlingsstatus) : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }

    /** Behandlingen er ikke lenger under beslutning hos utøvende beslutter. */
    data object BehandlingenErIkkeLengerUnderBeslutning : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst = Loggkontekst("behandlingen er ikke lenger under beslutning hos utøvende beslutter")
    }

    /** Klagebehandlingen som henger på rammebehandlingen kunne ikke settes på vent. */
    data class KunneIkkeSetteKlagebehandlingenPåVent(
        val underliggende: KanIkkeSetteKlagebehandlingPåVent,
    ) : KanIkkeSetteRammebehandlingPåVent {
        override val loggkontekst get() = Loggkontekst("klagebehandlingen kunne ikke settes på vent: $underliggende")
    }
}
