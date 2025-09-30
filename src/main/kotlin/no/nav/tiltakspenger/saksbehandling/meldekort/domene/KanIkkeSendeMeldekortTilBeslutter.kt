package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

sealed interface KanIkkeSendeMeldekortTilBeslutter {
    data class KanIkkeOppdatere(val underliggende: KanIkkeOppdatereMeldekort) : KanIkkeSendeMeldekortTilBeslutter
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortTilBeslutter
    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeSendeMeldekortTilBeslutter
}
