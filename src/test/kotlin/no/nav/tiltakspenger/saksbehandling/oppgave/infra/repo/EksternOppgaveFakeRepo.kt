package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.util.concurrent.ConcurrentHashMap

class EksternOppgaveFakeRepo : EksternOppgaveRepo {
    private val data = ConcurrentHashMap<OppgaveId, EksternOppgave>()

    override fun lagre(eksternOppgave: EksternOppgave, sessionContext: SessionContext?) {
        data.putIfAbsent(eksternOppgave.oppgaveId, eksternOppgave)
    }

    override fun hentForSakId(sakId: SakId): List<EksternOppgave> =
        data.values.filter { it.sakId == sakId }
            .sortedWith(compareBy<EksternOppgave> { it.opprettet }.thenBy { it.oppgaveId.toString() })
}
