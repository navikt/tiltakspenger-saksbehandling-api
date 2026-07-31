package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeIverksetteMeldekortbehandling {
    /** Kontrollsimuleringen som kjøres rett før iverksettelse feilet. */
    data class SimuleringFeil(val feil: KunneIkkeSimulere) : KanIkkeIverksetteMeldekortbehandling

    /** Kontrollsimuleringen eller simuleringen på behandlingen gir et resultat vi ikke kan utbetale. */
    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeIverksetteMeldekortbehandling

    data object SaksbehandlerOgBeslutterKanIkkeVæreLik : KanIkkeIverksetteMeldekortbehandling

    data object BehandlingenErIkkeUnderBeslutning : KanIkkeIverksetteMeldekortbehandling

    data object MåVæreBeslutterForMeldekortet : KanIkkeIverksetteMeldekortbehandling

    data object MeldeperiodeneErIkkeSisteVersjon : KanIkkeIverksetteMeldekortbehandling
}
