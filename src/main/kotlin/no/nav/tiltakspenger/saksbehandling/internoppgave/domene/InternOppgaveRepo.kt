package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface InternOppgaveRepo {
    fun hent(id: InternOppgaveId, sessionContext: SessionContext? = null): InternOppgave?

    fun hentÅpen(
        sakId: SakId,
        type: InternOppgavetype,
        nøkkel: String,
        sessionContext: SessionContext? = null,
    ): InternOppgave?

    fun hentForSak(sakId: SakId, sessionContext: SessionContext? = null): List<InternOppgave>

    /** Uløste oppgaver, inkludert tildelte, sortert etter opprettet og id. */
    fun hentUløste(limit: Int, offset: Int, sessionContext: SessionContext? = null): List<InternOppgave>

    /** Returnerer false dersom id eller kombinasjonen sak, type og nøkkel allerede har en åpen oppgave. */
    fun opprett(oppgave: InternOppgave, sessionContext: SessionContext? = null): Boolean

    /** Lagrer bare dersom den åpne oppgaven fortsatt har forventet versjon. */
    fun oppdater(oppgave: InternOppgave, forventetVersjon: Long, sessionContext: SessionContext? = null): Boolean
}
