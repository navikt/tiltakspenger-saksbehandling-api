package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.atomic.Atomic
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
    private val opprettedeUtenDuplikatkontroll = Atomic(mutableListOf<Pair<Fnr, Oppgavebehov>>())

    /** Oppgavene opprettet uten duplikatkontroll, i rekkefølge, slik at testene kan asserte på fnr og oppgavebehov. */
    val opprettedeOppgaverUtenDuplikatkontroll: List<Pair<Fnr, Oppgavebehov>> get() = opprettedeUtenDuplikatkontroll.get().toList()

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
        opprettedeUtenDuplikatkontroll.get().add(fnr to oppgavebehov)
        return ObjectMother.oppgaveId().right()
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Either<HttpKlientError, Boolean> {
        return erFerdigstiltResponse.right()
    }
}
