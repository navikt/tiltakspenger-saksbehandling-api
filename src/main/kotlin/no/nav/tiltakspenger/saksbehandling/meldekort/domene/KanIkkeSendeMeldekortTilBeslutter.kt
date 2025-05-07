package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId

sealed interface KanIkkeSendeMeldekortTilBeslutter {
    data class MåVæreSaksbehandler(val roller: Saksbehandlerroller) : KanIkkeSendeMeldekortTilBeslutter
    data class KunneIkkeHenteSak(val underliggende: KunneIkkeHenteSakForSakId) : KanIkkeSendeMeldekortTilBeslutter
    data class KanIkkeOppdatere(val underliggende: KanIkkeOppdatereMeldekort) : KanIkkeSendeMeldekortTilBeslutter
    data object MåVæreSaksbehandlerForMeldekortet : KanIkkeSendeMeldekortTilBeslutter
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortTilBeslutter
}
