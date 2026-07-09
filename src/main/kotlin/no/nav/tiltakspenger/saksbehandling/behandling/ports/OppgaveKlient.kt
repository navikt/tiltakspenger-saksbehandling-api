package no.nav.tiltakspenger.saksbehandling.behandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

/**
 * Port for oppgaver i oppgavesystemet (Gosys).
 * Feil på HTTP-nivå returneres som [HttpKlientError]; kallende service/jobb håndterer og logger dem (via `HttpKlientError.loggFeil`).
 * Et ukjent [Oppgavebehov] for metoden er en programmeringsfeil og kaster [IllegalArgumentException].
 */
interface OppgaveKlient {
    suspend fun opprettOppgave(fnr: Fnr, journalpostId: JournalpostId, oppgavebehov: Oppgavebehov): Either<HttpKlientError, OppgaveId>

    suspend fun ferdigstillOppgave(oppgaveId: OppgaveId): Either<HttpKlientError, Unit>

    suspend fun opprettOppgaveUtenDuplikatkontroll(
        fnr: Fnr,
        oppgavebehov: Oppgavebehov,
        tilleggstekst: String? = null,
    ): Either<HttpKlientError, OppgaveId>

    suspend fun erFerdigstilt(oppgaveId: OppgaveId): Either<HttpKlientError, Boolean>
}

enum class Oppgavebehov {
    ENDRET_TILTAKDELTAKER,
    NYTT_MELDEKORT,
    FATT_BARN,
    DOED,
    NY_SOKNAD,
    ADRESSEBESKYTTELSE,
}
