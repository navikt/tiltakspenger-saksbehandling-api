package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId

sealed interface KanIkkeOppdatereMeldekort {
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeOppdatereMeldekort
    data class MåVæreSaksbehandler(val roller: Saksbehandlerroller) : KanIkkeOppdatereMeldekort
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeOppdatereMeldekort
    data class KunneIkkeHenteSak(val underliggende: KunneIkkeHenteSakForSakId) : KanIkkeOppdatereMeldekort
}
