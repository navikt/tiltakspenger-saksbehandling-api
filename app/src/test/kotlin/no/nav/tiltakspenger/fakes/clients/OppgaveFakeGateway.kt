package no.nav.tiltakspenger.fakes.clients

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.felles.OppgaveId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.Oppgavebehov

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
