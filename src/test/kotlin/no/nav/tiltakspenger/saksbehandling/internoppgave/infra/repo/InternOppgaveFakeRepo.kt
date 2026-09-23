package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Dialoginnlegg
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgave
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype

class InternOppgaveFakeRepo : InternOppgaveRepo {
    private val oppgaver = mutableMapOf<InternOppgaveId, InternOppgave>()
    private val rekkefølge = compareBy<InternOppgave> { it.opprettet }.thenBy { it.id.toString() }

    @Synchronized
    override fun hent(id: InternOppgaveId, sessionContext: SessionContext?): InternOppgave? = oppgaver[id]

    @Synchronized
    override fun hentÅpen(
        sakId: SakId,
        type: InternOppgavetype,
        nøkkel: String,
        sessionContext: SessionContext?,
    ): InternOppgave? = oppgaver.values.singleOrNull {
        it.sakId == sakId && it.type == type && it.nøkkel == nøkkel && !it.erLøst
    }

    @Synchronized
    override fun hentForSak(sakId: SakId, sessionContext: SessionContext?): List<InternOppgave> =
        oppgaver.values.filter { it.sakId == sakId }.sortedWith(rekkefølge)

    @Synchronized
    override fun hentUløste(limit: Int, offset: Int, sessionContext: SessionContext?): List<InternOppgave> {
        require(limit in 1..100 && offset >= 0)
        return oppgaver.values.filterNot { it.erLøst }.sortedWith(rekkefølge).drop(offset).take(limit)
    }

    @Synchronized
    override fun opprett(oppgave: InternOppgave, sessionContext: SessionContext?): Boolean {
        if (oppgave.id in oppgaver) return false
        if (!oppgave.erLøst && hentÅpen(oppgave.sakId, oppgave.type, oppgave.nøkkel, sessionContext) != null) return false
        oppgaver[oppgave.id] = oppgave
        return true
    }

    @Synchronized
    override fun oppdater(oppgave: InternOppgave, forventetVersjon: Long, sessionContext: SessionContext?): Boolean {
        require(oppgave.versjon == forventetVersjon + 1)
        val eksisterende = oppgaver[oppgave.id] ?: return false
        if (eksisterende.erLøst || eksisterende.versjon != forventetVersjon) return false
        if (eksisterende.sakId != oppgave.sakId || eksisterende.type != oppgave.type || eksisterende.nøkkel != oppgave.nøkkel) return false
        // Som Postgres-repoet skriver ikke oppdater dialogen.
        oppgaver[oppgave.id] = oppgave.medDialog(eksisterende.dialog)
        return true
    }

    @Synchronized
    override fun leggTilDialoginnlegg(id: InternOppgaveId, innlegg: Dialoginnlegg, sessionContext: SessionContext?): Boolean {
        val eksisterende = oppgaver[id] ?: return false
        if (eksisterende.erLøst) return false
        oppgaver[id] = eksisterende.medDialog(eksisterende.dialog + innlegg)
        return true
    }

    private fun InternOppgave.medDialog(dialog: List<Dialoginnlegg>) = InternOppgave(
        id = id,
        sakId = sakId,
        grunnlag = grunnlag,
        opprettet = opprettet,
        sistEndret = sistEndret,
        versjon = versjon,
        saksbehandler = saksbehandler,
        løsning = løsning,
        dialog = dialog,
    )
}
