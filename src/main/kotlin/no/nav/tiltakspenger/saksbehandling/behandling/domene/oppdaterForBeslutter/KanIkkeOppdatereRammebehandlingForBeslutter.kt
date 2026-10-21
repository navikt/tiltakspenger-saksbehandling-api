package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

sealed interface KanIkkeOppdatereRammebehandlingForBeslutter : Loggbar {
    data class RammebehandlingenErIkkeUnderBeslutning(val status: Rammebehandlingsstatus) : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }

    data object BeslutterMåVæreTildeltRammebehandlingen : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst = Loggkontekst("utøvende bruker er ikke beslutteren som er tildelt behandlingen")
    }

    data object RammebehandlingenErAlleredeGodkjent : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst = Loggkontekst("behandlingen er allerede godkjent")
    }

    data object RammebehandlingenErSattPåVent : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst = Loggkontekst("behandlingen er satt på vent")
    }

    data object MåHaBegrunnelseForÅJournalføreNotat : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst = Loggkontekst("begrunnelsen for vilkårsvurderingen mangler")
    }

    data object MåHaBegrunnelseForBarnetilleggForÅJournalføreNotat : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst = Loggkontekst("begrunnelsen for barnetillegget mangler")
    }

    data object BehandlingenErIkkeLengerUnderBeslutning : KanIkkeOppdatereRammebehandlingForBeslutter {
        override val loggkontekst = Loggkontekst("behandlingen er ikke lenger under beslutning")
    }
}
