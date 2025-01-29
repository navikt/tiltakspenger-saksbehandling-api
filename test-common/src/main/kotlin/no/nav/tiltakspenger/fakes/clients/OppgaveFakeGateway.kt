package no.nav.tiltakspenger.fakes.clients

import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway

class OppgaveFakeGateway : OppgaveGateway {
    override suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId): Int {
        return 1
    }

    override suspend fun ferdigstillOppgave(oppgaveId: Int) {
    }
}
