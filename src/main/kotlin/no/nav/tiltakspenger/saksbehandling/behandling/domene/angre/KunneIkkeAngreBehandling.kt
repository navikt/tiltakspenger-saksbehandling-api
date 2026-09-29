package no.nav.tiltakspenger.saksbehandling.behandling.domene.angre

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

sealed interface KunneIkkeAngreBehandling : Loggbar {
    /** Behandlingen kan kun angres av saksbehandleren som er tildelt behandlingen. */
    data object MåVæreSaksbehandlerForBehandlingen : KunneIkkeAngreBehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke saksbehandleren som er tildelt behandlingen")
    }

    data class BehandlingenErIEnTilstandSomIkkeTillaterÅAngre(val status: Rammebehandlingsstatus) : KunneIkkeAngreBehandling {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }

    data object BehandlingenErSattPåVent : KunneIkkeAngreBehandling {
        override val loggkontekst = Loggkontekst("behandlingen er satt på vent")
    }

    /**
     * Behandlingen ble endret av noen andre mellom innlasting og lagring, for eksempel iverksatt eller underkjent av beslutteren.
     * Returneres når angre-oppdateringen i databasen ikke treffer noen rader.
     */
    data object BehandlingenErIkkeLengerSendtTilBeslutning : KunneIkkeAngreBehandling {
        override val loggkontekst = Loggkontekst("behandlingen er ikke lenger sendt til beslutning")
    }
}
