package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

sealed interface KanIkkeSendeMeldekortbehandlingTilBeslutter {
    data class KanIkkeOppdatere(val underliggende: KanIkkeOppdatereMeldekortbehandling) : KanIkkeSendeMeldekortbehandlingTilBeslutter
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortbehandlingTilBeslutter
    data class UtbetalingStøttesIkke(val feil: KanIkkeIverksetteUtbetaling) : KanIkkeSendeMeldekortbehandlingTilBeslutter
}
