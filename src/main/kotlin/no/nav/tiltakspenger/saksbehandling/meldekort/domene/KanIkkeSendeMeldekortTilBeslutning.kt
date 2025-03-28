package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId

sealed interface KanIkkeSendeMeldekortTilBeslutning {
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortTilBeslutning
    data class MåVæreSaksbehandler(val roller: Saksbehandlerroller) : KanIkkeSendeMeldekortTilBeslutning
    data class ForMangeDagerUtfylt(
        val maksDagerMedTiltakspengerForPeriode: Int,
        val antallDagerUtfylt: Int,
    ) : KanIkkeSendeMeldekortTilBeslutning
    data class KunneIkkeHenteSak(val underliggende: KunneIkkeHenteSakForSakId) : KanIkkeSendeMeldekortTilBeslutning
    data object KanIkkeEndreDagFraSperret : KanIkkeSendeMeldekortTilBeslutning
    data object KanIkkeEndreDagTilSperret : KanIkkeSendeMeldekortTilBeslutning
    data object InnsendteDagerMåMatcheMeldeperiode : KanIkkeSendeMeldekortTilBeslutning
}
