package no.nav.tiltakspenger.saksbehandling.behandling.domene.hentTilbake

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

sealed interface KanIkkeHenteTilbakeRammebehandling : Loggbar {

    data class UgyldigStatus(val status: Rammebehandlingsstatus) : KanIkkeHenteTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst(melding = "Rammebehandlingen må være klar til beslutning. Status er $status")
    }

    data object MåVæreSaksbehandler : KanIkkeHenteTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst(melding = "Utøvende saksbehandler mangler saksbehandlerrollen")
    }

    data object MåVæreSøknadsbehandling : KanIkkeHenteTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst(melding = "Kun søknadsbehandlinger kan hentes tilbake, men rammebehandlingen er en revurdering")
    }

    data object ErIkkeAutomatiskBehandlet : KanIkkeHenteTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst(melding = "Rammebehandlingen må være tildelt den automatiske saksbehandleren for å kunne hentes tilbake")
    }

    data object BehandlingenErSattPåVent : KanIkkeHenteTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst(melding = "Rammebehandlingen er satt på vent av en beslutter og kan ikke hentes tilbake")
    }

    data object BehandlingenErEndret : KanIkkeHenteTilbakeRammebehandling {
        override val loggkontekst = Loggkontekst(melding = "Rammebehandlingen er endret siden den ble lastet, og er ikke lenger klar til beslutning hos den automatiske saksbehandleren")
    }
}
