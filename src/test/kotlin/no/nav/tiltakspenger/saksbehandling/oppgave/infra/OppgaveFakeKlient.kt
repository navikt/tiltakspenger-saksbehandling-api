package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

class OppgaveFakeKlient : OppgaveKlient {
    override suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): OppgaveId {
        return ObjectMother.oppgaveId()
    }

    override suspend fun ferdigstillOppgave(oppgaveId: OppgaveId) {
    }

    override suspend fun opprettOppgaveUtenDuplikatkontroll(
        fnr: Fnr,
        oppgavebehov: Oppgavebehov,
        tilleggstekst: String?,
    ): OppgaveId {
        return ObjectMother.oppgaveId()
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Boolean {
        return true
    }
}
