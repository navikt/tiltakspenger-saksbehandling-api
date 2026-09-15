package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/**
 * Mulige grunner til at en avbrutt søknadsbehandling ikke kan gjenåpnes.
 * Alle utledes av [kanGjenåpneSøknadsbehandling], som kjøres før noe endres.
 */
sealed interface KanIkkeGjenåpneSøknadsbehandling : Loggbar {
    /** Behandlingen finnes ikke på saken. */
    data class FantIkkeBehandling(val behandlingId: RammebehandlingId) : KanIkkeGjenåpneSøknadsbehandling {
        override val loggkontekst get() = Loggkontekst("fant ikke behandlingen $behandlingId på saken")
    }

    /** Det er kun søknadsbehandlinger som kan gjenåpnes - en revurdering har ingen søknad å ta opp igjen. */
    data object BehandlingenErIkkeEnSøknadsbehandling : KanIkkeGjenåpneSøknadsbehandling {
        override val loggkontekst = Loggkontekst("behandlingen er ikke en søknadsbehandling")
    }

    /** Behandlingen er ikke avbrutt, og det er derfor ingenting å gjenåpne. */
    data class BehandlingenErIkkeAvbrutt(val status: Rammebehandlingsstatus) : KanIkkeGjenåpneSøknadsbehandling {
        override val loggkontekst get() = Loggkontekst("behandlingen har status $status og er ikke avbrutt")
    }

    /** Søknaden har allerede en søknadsbehandling som ikke er avbrutt, så det er ingenting å ta opp igjen. */
    data object SøknadenHarEnAktivBehandling : KanIkkeGjenåpneSøknadsbehandling {
        override val loggkontekst = Loggkontekst("søknaden har allerede en søknadsbehandling som ikke er avbrutt")
    }

    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KanIkkeGjenåpneSøknadsbehandling {
        override val loggkontekst = Loggkontekst("utøvende bruker mangler saksbehandlerrolle")
    }
}
