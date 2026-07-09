package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

class OppgaveFakeKlient(
    var erFerdigstiltResponse: Boolean = true,
) : OppgaveKlient {
    override suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): Either<HttpKlientError, OppgaveId> {
        return ObjectMother.oppgaveId().right()
    }

    override suspend fun ferdigstillOppgave(oppgaveId: OppgaveId): Either<HttpKlientError, Unit> {
        return Unit.right()
    }

    override suspend fun opprettOppgaveUtenDuplikatkontroll(
        fnr: Fnr,
        oppgavebehov: Oppgavebehov,
        tilleggstekst: String?,
    ): Either<HttpKlientError, OppgaveId> {
        return ObjectMother.oppgaveId().right()
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Either<HttpKlientError, Boolean> {
        return erFerdigstiltResponse.right()
    }
}
