package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.KanIkkeTaKlagebehandling

sealed interface KunneIkkeTaBehandling : Loggbar {
    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KunneIkkeTaBehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler saksbehandlerrolle")
    }

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KunneIkkeTaBehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler beslutterrolle")
    }

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

/**
 * Kaster [no.nav.tiltakspenger.saksbehandling.felles.TilgangException] dersom feilen skyldes at [saksbehandler] mangler en rolle.
 * Manglende rolle er en tilgangsfeil (403), ikke en tilstandsfeil, og skal derfor ikke returneres som en venstre-verdi.
 */
fun KunneIkkeTaBehandling.kastVedManglendeRolle(saksbehandler: Saksbehandler) {
    when (this) {
        KunneIkkeTaBehandling.MåVæreSaksbehandler -> krevSaksbehandlerRolle(saksbehandler)
        KunneIkkeTaBehandling.MåVæreBeslutter -> krevBeslutterRolle(saksbehandler)
        else -> Unit
    }
}
