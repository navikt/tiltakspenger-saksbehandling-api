package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.Fnr

interface OppgaveGateway {
    suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): OppgaveId
    suspend fun ferdigstillOppgave(oppgaveId: OppgaveId)
    suspend fun opprettOppgaveUtenDuplikatkontroll(fnr: Fnr, oppgavebehov: Oppgavebehov): OppgaveId
}

enum class Oppgavebehov {
    NY_SOKNAD,
    ENDRET_TILTAKDELTAKER,
}
