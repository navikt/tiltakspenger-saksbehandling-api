package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

interface OppgaveKlient {
    suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): OppgaveId
    suspend fun ferdigstillOppgave(oppgaveId: OppgaveId)
    suspend fun opprettOppgaveUtenDuplikatkontroll(fnr: Fnr, oppgavebehov: Oppgavebehov): OppgaveId
    suspend fun erFerdigstilt(oppgaveId: OppgaveId): Boolean
}

enum class Oppgavebehov {
    ENDRET_TILTAKDELTAKER,
    NYTT_MELDEKORT,
    FATT_BARN,
    DOED,
}
