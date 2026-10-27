package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId

sealed interface KanIkkeOppdatereMeldekortbehandling {
    data object MeldekortperiodenKanIkkeVæreFremITid : KanIkkeOppdatereMeldekortbehandling

    data object IngenDagerGirRett : KanIkkeOppdatereMeldekortbehandling

    data class ForMangeDagerUtfylt(
        val antallUtfylt: Int,
        val maksAntall: Int,
    ) : KanIkkeOppdatereMeldekortbehandling

    /** En meldeperiodekjede kan bare være omfattet av én åpen meldekortbehandling om gangen. */
    data class KjedeErUnderBehandling(
        val kjedeIder: Set<MeldeperiodeKjedeId>,
    ) : KanIkkeOppdatereMeldekortbehandling
}
