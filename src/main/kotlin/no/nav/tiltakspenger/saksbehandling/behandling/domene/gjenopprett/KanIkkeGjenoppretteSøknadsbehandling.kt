package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/**
 * Mulige grunner til at en avbrutt søknadsbehandling ikke kan gjenopprettes.
 * Alle utledes av [kanGjenoppretteSøknadsbehandling], som kjøres før noe endres.
 */
sealed interface KanIkkeGjenoppretteSøknadsbehandling : Loggbar {
    /** Behandlingen finnes ikke på saken. */
    data class FantIkkeBehandling(val behandlingId: RammebehandlingId) : KanIkkeGjenoppretteSøknadsbehandling {
        override val loggkontekst get() = Loggkontekst("fant ikke behandlingen $behandlingId på saken")
    }

    /** Det er kun søknadsbehandlinger som kan gjenopprettes - en revurdering har ingen søknad å ta opp igjen. */
    data object BehandlingenErIkkeEnSøknadsbehandling : KanIkkeGjenoppretteSøknadsbehandling {
        override val loggkontekst = Loggkontekst("behandlingen er ikke en søknadsbehandling")
    }

    /** Behandlingen er ikke avbrutt, og det er derfor ingenting å gjenopprette. */
    data class BehandlingenErIkkeAvbrutt(val status: Rammebehandlingsstatus) : KanIkkeGjenoppretteSøknadsbehandling {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status og er ikke avbrutt")
    }

    /** Søknaden har allerede en søknadsbehandling som ikke er avbrutt, så det er ingenting å ta opp igjen. */
    data object SøknadenHarEnAktivBehandling : KanIkkeGjenoppretteSøknadsbehandling {
        override val loggkontekst = Loggkontekst("søknaden har allerede en søknadsbehandling som ikke er avbrutt")
    }

    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeGjenoppretteSøknadsbehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler saksbehandlerrolle")
    }
}
