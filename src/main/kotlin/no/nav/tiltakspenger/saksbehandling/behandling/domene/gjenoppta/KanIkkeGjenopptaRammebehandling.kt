package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta

import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.KanIkkeGjenopptaKlagebehandling

/**
 * Mulige grunner til at en rammebehandling ikke kan gjenopptas.
 *
 * De fire første utledes av `Rammebehandling.kanGjenoppta`, og er de eneste som kan oppstå før behandlingen er endret.
 * De to siste kan bare oppstå underveis i `Rammebehandling.gjenoppta`, og har derfor ingen tilsvarende forhåndssjekk.
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

    /** Klagebehandlingen som henger på rammebehandlingen kunne ikke gjenopptas. */
    data class KunneIkkeGjenopptaKlagebehandlingen(
        val underliggende: KanIkkeGjenopptaKlagebehandling,
    ) : KanIkkeGjenopptaRammebehandling {
        override val loggkontekst get() = Loggkontekst("klagebehandlingen kunne ikke gjenopptas: $underliggende")
    }

    /** Saksopplysningene ble hentet på nytt ved gjenopptak, men kunne ikke legges på behandlingen. */
    data class KunneIkkeOppdatereSaksopplysningene(
        val underliggende: KunneIkkeOppdatereSaksopplysninger,
    ) : KanIkkeGjenopptaRammebehandling {
        override val loggkontekst get() = Loggkontekst("saksopplysningene kunne ikke oppdateres: $underliggende")
    }
}
