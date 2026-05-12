package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

sealed interface KanIkkeTaMeldekortbehandling {
    data object MeldekortbehandlingFinnesIkke : KanIkkeTaMeldekortbehandling
    data object HarAlleredeSaksbehandler : KanIkkeTaMeldekortbehandling
    data object HarAlleredeBeslutter : KanIkkeTaMeldekortbehandling
    data object BeslutterKanIkkeVæreSammeSomSaksbehandler : KanIkkeTaMeldekortbehandling
    data class UgyldigStatus(val status: MeldekortbehandlingStatus) : KanIkkeTaMeldekortbehandling
}
