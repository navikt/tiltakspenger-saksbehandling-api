package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

sealed interface KanIkkeAngreMeldekortbehandling : Loggbar {
    // Meldekortbehandlingen kan kun angres av saksbehandleren som er tildelt behandlingen

    data object MåVæreSammeSaksbehandlerForÅAngreMeldekortbehandlingen : KanIkkeAngreMeldekortbehandling {
        override val loggkontekst: Loggkontekst = Loggkontekst("utøvende bruker er ikke saksbehandleren som er tildelt meldekortbehandlingen")
    }

    data class MeldekortbehandlingenErIEnTilstandSomIkkeTillaterÅAngre(val status: MeldekortbehandlingStatus) : KanIkkeAngreMeldekortbehandling {
        override val loggkontekst: Loggkontekst = Loggkontekst("meldekortbehandlingen har status $status")
    }

    data object MeldekortbehandlingFinnesIkke : KanIkkeAngreMeldekortbehandling {
        override val loggkontekst: Loggkontekst = Loggkontekst("meldekortbehandlingen finnes ikke")
    }

    data object MeldekortbehandlingErIkkeLengerSendtTilBeslutning : KanIkkeAngreMeldekortbehandling {
        override val loggkontekst: Loggkontekst = Loggkontekst("meldekortbehandlingen er ikke lenger sendt til beslutning")
    }

    data object MeldekortbehandlingenErSattPåVent : KanIkkeAngreMeldekortbehandling {
        override val loggkontekst: Loggkontekst = Loggkontekst("meldekortbehandlingen er satt på vent")
    }
}
