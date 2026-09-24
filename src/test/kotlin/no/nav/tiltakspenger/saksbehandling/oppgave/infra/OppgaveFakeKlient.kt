package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class OppgaveFakeKlient(
    var erFerdigstiltResponse: Boolean = true,
    private val genererOppgaveId: () -> OppgaveId = { OppgaveId(UUID.randomUUID().toString()) },
) : OppgaveKlient {
    private val opprettedeUtenDuplikatkontroll = Atomic(mutableListOf<OppgaveUtenDuplikatkontroll>())
    private val oppgaverMedJournalpost = ConcurrentHashMap<Pair<JournalpostId, Oppgavebehov>, OppgaveId>()
    private val oppgaveIder = CopyOnWriteArrayList<OppgaveId>()

    val opprettedeOppgaveIder: List<OppgaveId> get() = oppgaveIder.toList()

    var opprettOppgaveResponse: Either<HttpKlientError, OppgaveId>? = null
    var opprettOppgaveUtenDuplikatkontrollResponse: Either<HttpKlientError, OppgaveId>? = null

    /** Oppgavene opprettet uten duplikatkontroll, i rekkefølge, slik at testene kan asserte på fnr og oppgavebehov. */
    val opprettedeOppgaverUtenDuplikatkontroll: List<Pair<Fnr, Oppgavebehov>> get() = opprettedeUtenDuplikatkontroll.get().map { it.fnr to it.oppgavebehov }
    val opprettedeOppgavetekster: List<String?> get() = opprettedeUtenDuplikatkontroll.get().map { it.tilleggstekst }

    override suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): Either<HttpKlientError, OppgaveId> {
        opprettOppgaveResponse?.let { return it }
        return oppgaverMedJournalpost.computeIfAbsent(journalpostId to oppgavebehov) {
            genererOppgaveId().also { oppgaveIder.add(it) }
        }.right()
    }

    override suspend fun ferdigstillOppgave(oppgaveId: OppgaveId): Either<HttpKlientError, Unit> {
        return Unit.right()
    }

    override suspend fun opprettOppgaveUtenDuplikatkontroll(
        fnr: Fnr,
        oppgavebehov: Oppgavebehov,
        tilleggstekst: String?,
    ): Either<HttpKlientError, OppgaveId> {
        opprettedeUtenDuplikatkontroll.get().add(OppgaveUtenDuplikatkontroll(fnr, oppgavebehov, tilleggstekst))
        opprettOppgaveUtenDuplikatkontrollResponse?.let { return it }
        return genererOppgaveId().also { oppgaveIder.add(it) }.right()
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Either<HttpKlientError, Boolean> {
        return erFerdigstiltResponse.right()
    }
}

private data class OppgaveUtenDuplikatkontroll(
    val fnr: Fnr,
    val oppgavebehov: Oppgavebehov,
    val tilleggstekst: String?,
)
