package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.KanIkkeTaKlagebehandling

sealed interface KunneIkkeTaBehandling : Loggbar {
    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSammePåBehandling : KunneIkkeTaBehandling {
        override val loggkontekst = Loggkontekst("saksbehandler og beslutter kan ikke være den samme på behandlingen")
    }

    data object BehandlingenHarEksisterendeSaksbehandler : KunneIkkeTaBehandling {
        override val loggkontekst = Loggkontekst("behandlingen har allerede en saksbehandler")
    }

    data object BehandlingenHarEksisterendeBeslutter : KunneIkkeTaBehandling {
        override val loggkontekst = Loggkontekst("behandlingen har allerede en beslutter")
    }

    data class BehandlingenErIEnTilstandSomIkkeTillaterÅTaBehandling(val status: Rammebehandlingsstatus) : KunneIkkeTaBehandling {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }

    data class FeilVedKlagebehandling(val originalfeil: KanIkkeTaKlagebehandling) : KunneIkkeTaBehandling {
        override val loggkontekst get() = Loggkontekst("kunne ikke ta tilknyttet klagebehandling: $originalfeil")
    }
}
