package no.nav.tiltakspenger.saksbehandling.fakes.clients

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.Oppgavebehov

class OppgaveFakeGateway : OppgaveGateway {
    override suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): OppgaveId {
        return ObjectMother.oppgaveId()
    }

    override suspend fun ferdigstillOppgave(oppgaveId: OppgaveId) {
    }

    override suspend fun opprettOppgaveUtenDuplikatkontroll(fnr: Fnr, oppgavebehov: Oppgavebehov): OppgaveId {
        return ObjectMother.oppgaveId()
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Boolean {
        return true
    }
}
