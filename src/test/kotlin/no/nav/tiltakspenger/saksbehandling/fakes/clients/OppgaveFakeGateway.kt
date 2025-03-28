package no.nav.tiltakspenger.saksbehandling.fakes.clients

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

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
