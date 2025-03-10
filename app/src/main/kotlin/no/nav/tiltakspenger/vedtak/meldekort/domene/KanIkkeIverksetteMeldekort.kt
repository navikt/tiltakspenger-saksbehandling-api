package no.nav.tiltakspenger.vedtak.meldekort.domene

import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.KunneIkkeHenteSakForSakId

sealed interface KanIkkeIverksetteMeldekort {
    data class MåVæreBeslutter(val roller: Saksbehandlerroller) : KanIkkeIverksetteMeldekort
    data object SaksbehandlerOgBeslutterKanIkkeVæreLik : KanIkkeIverksetteMeldekort
    data class KunneIkkeHenteSak(val underliggende: KunneIkkeHenteSakForSakId) : KanIkkeIverksetteMeldekort
}
