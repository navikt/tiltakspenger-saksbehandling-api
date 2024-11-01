package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Roller

sealed interface KanIkkeSendeMeldekortTilBeslutter {
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeSendeMeldekortTilBeslutter
    data class MåVæreSaksbehandler(val roller: Roller) : KanIkkeSendeMeldekortTilBeslutter
    data class ForMangeDagerUtfylt(val antallDagerForMeldeperiode: Int, val antallDagerUtfylt: Int) : KanIkkeSendeMeldekortTilBeslutter
}
