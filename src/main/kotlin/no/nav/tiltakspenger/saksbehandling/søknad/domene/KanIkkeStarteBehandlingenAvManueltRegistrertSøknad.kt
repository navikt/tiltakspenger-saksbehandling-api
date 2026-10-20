package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

sealed interface KanIkkeStarteBehandlingenAvManueltRegistrertSøknad : Loggbar {

    data class JournalpostFinnesIkke(val id: JournalpostId) : KanIkkeStarteBehandlingenAvManueltRegistrertSøknad {
        override val loggkontekst: Loggkontekst = Loggkontekst("Journalpost $id finnes ikke")
    }

    data class JournalpostManglerOpprettsdato(val id: JournalpostId) : KanIkkeStarteBehandlingenAvManueltRegistrertSøknad {
        override val loggkontekst: Loggkontekst = Loggkontekst("Journalpost $id mangler datoOpprettet")
    }

    data class JournalpostTilhørerEnAnnenBruker(val id: JournalpostId) : KanIkkeStarteBehandlingenAvManueltRegistrertSøknad {
        override val loggkontekst: Loggkontekst = Loggkontekst("Journalpost $id tilhører en annen bruker")
    }

    data object EnManuellDatoKrevesNårDetIkkeErMigreringFraArena : KanIkkeStarteBehandlingenAvManueltRegistrertSøknad {
        override val loggkontekst: Loggkontekst = Loggkontekst("En manuell registrert søknad må ha en dato når den ikke overføres fra Arena")
    }
}
