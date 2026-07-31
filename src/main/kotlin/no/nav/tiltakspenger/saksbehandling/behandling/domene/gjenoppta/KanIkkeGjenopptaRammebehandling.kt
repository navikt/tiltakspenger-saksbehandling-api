package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle

/**
 * Mulige grunner til at en rammebehandling ikke kan gjenopptas.
 *
 * Se `Rammebehandling.kanGjenoppta`.
 */
sealed interface KanIkkeGjenopptaRammebehandling : Loggbar {
    /** Behandlingen er ikke satt på vent. */
    data object BehandlingenErIkkePåVent : KanIkkeGjenopptaRammebehandling {
        override val loggkontekst = Loggkontekst("behandlingen er ikke satt på vent")
    }

    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeGjenopptaRammebehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler saksbehandlerrolle")
    }

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KanIkkeGjenopptaRammebehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler beslutterrolle")
    }

    /** Behandlingen er i en status som ikke kan gjenopptas. */
    data class UgyldigStatus(val status: Rammebehandlingsstatus) : KanIkkeGjenopptaRammebehandling {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status")
    }
}

/**
 * Kaster [no.nav.tiltakspenger.saksbehandling.felles.TilgangException] dersom feilen skyldes at [saksbehandler] mangler en rolle.
 * Manglende rolle er en tilgangsfeil (403), ikke en tilstandsfeil.
 */
fun KanIkkeGjenopptaRammebehandling.kastVedManglendeRolle(saksbehandler: Saksbehandler) {
    when (this) {
        KanIkkeGjenopptaRammebehandling.MåVæreSaksbehandler -> krevSaksbehandlerRolle(saksbehandler)
        KanIkkeGjenopptaRammebehandling.MåVæreBeslutter -> krevBeslutterRolle(saksbehandler)
        else -> Unit
    }
}
