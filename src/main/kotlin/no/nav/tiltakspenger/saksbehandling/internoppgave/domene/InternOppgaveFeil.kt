package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

sealed interface InternOppgaveFeil {
    data object FantIkkeOppgave : InternOppgaveFeil

    data object OppgavenErEndret : InternOppgaveFeil

    data object OppgavenErLøst : InternOppgaveFeil

    data object AlleredeTildelt : InternOppgaveFeil

    data object IkkeEier : InternOppgaveFeil

    data object IkkeTildelt : InternOppgaveFeil

    data object KanIkkeOvertaFraSegSelv : InternOppgaveFeil

    data object AnnetGrunnlag : InternOppgaveFeil
}
