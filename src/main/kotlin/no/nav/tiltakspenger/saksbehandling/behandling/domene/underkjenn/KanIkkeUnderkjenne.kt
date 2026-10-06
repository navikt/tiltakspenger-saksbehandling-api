package no.nav.tiltakspenger.saksbehandling.behandling.domene.underkjenn

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

sealed interface KanIkkeUnderkjenne : Loggbar {
    data object ManglerBegrunnelse : KanIkkeUnderkjenne {
        override val loggkontekst = Loggkontekst("begrunnelse mangler")
    }

    /**
     * Behandlingen er ikke lenger under beslutning, for eksempel fordi saksbehandleren har angret sendingen til beslutning eller den allerede er underkjent.
     * Domenet kan ikke skille mellom årsakene, så feilen bærer bare statusen.
     */
    data class RammebehandlingenErIkkeUnderBeslutning(val status: Rammebehandlingsstatus) : KanIkkeUnderkjenne {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }

    data object BeslutterMåVæreTildeltRammebehandlingen : KanIkkeUnderkjenne {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke beslutteren som er tildelt behandlingen")
    }

    data object RammebehandlingenErAlleredeGodkjent : KanIkkeUnderkjenne {
        override val loggkontekst = Loggkontekst("behandlingen er allerede godkjent")
    }

    data object RammebehandlingenErSattPåVent : KanIkkeUnderkjenne {
        override val loggkontekst = Loggkontekst("behandlingen er satt på vent")
    }

    data object BehandlingenErIkkeLengerUnderBeslutning : KanIkkeUnderkjenne {
        override val loggkontekst = Loggkontekst("behandlingen er ikke lenger under beslutning")
    }
}
