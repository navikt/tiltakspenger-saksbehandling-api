package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.Fnr

interface OppgaveGateway {
    suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId): Int
    suspend fun ferdigstillOppgave(oppgaveId: Int)
}
