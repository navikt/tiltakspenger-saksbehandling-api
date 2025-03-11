package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId

interface OppgaveGateway {
    suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): OppgaveId
    suspend fun ferdigstillOppgave(oppgaveId: OppgaveId)
    suspend fun opprettOppgaveUtenDuplikatkontroll(fnr: Fnr, oppgavebehov: Oppgavebehov): OppgaveId
    suspend fun erFerdigstilt(oppgaveId: OppgaveId): Boolean
}

enum class Oppgavebehov {
    ENDRET_TILTAKDELTAKER,
    NY_SOKNAD,
    NYTT_MELDEKORT,
}
