package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.time.LocalDateTime
import java.util.UUID

interface PersonhendelseRepo {
    fun hent(sakId: SakId): List<Personhendelse>

    fun hent(id: UUID): Personhendelse?

    fun hentMedOppgaveId(id: UUID): PersonhendelseMedOppgaveId?

    fun hentIderUtenOppgave(): List<UUID>

    /**
     * Henter kun de hvor `oppgave_sist_sjekket` er null eller eldre enn [oppgaveSistSjekket].
     * Grensen er en policy og settes av kalleren; repoet kjenner ingen standardverdi.
     */
    fun hentIderMedOppgave(oppgaveSistSjekket: LocalDateTime): List<UUID>

    fun lagre(personhendelse: Personhendelse)

    fun slett(id: UUID)

    fun lagreOppgaveId(id: UUID, oppgaveId: OppgaveId)

    fun oppdaterOppgaveSistSjekket(id: UUID)
}
