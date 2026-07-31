package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeSendeMeldekortbehandlingTilBeslutter {
    /** Kontrollsimuleringen som kjøres når behandlingen sendes til beslutter feilet. */
    data class SimuleringFeil(val feil: KunneIkkeSimulere) : KanIkkeSendeMeldekortbehandlingTilBeslutter

    data class KanIkkeOppdatere(val underliggende: KanIkkeOppdatereMeldekortbehandling) : KanIkkeSendeMeldekortbehandlingTilBeslutter

    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortbehandlingTilBeslutter

    data object MeldeperiodeneErIkkeSisteVersjon : KanIkkeSendeMeldekortbehandlingTilBeslutter

    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeSendeMeldekortbehandlingTilBeslutter

    data object MeldeperiodeneErIkkeFullstendigUtfylt : KanIkkeSendeMeldekortbehandlingTilBeslutter
}
