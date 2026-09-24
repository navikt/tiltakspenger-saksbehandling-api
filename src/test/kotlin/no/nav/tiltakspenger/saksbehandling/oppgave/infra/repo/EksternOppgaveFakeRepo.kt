package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.LagretEksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.util.concurrent.ConcurrentHashMap

class EksternOppgaveFakeRepo : EksternOppgaveRepo {
    private val data = ConcurrentHashMap<OppgaveId, LagretEksternOppgave>()

    override fun lagre(eksternOppgave: EksternOppgave, sessionContext: SessionContext?) {
        data.putIfAbsent(
            eksternOppgave.oppgaveId,
            LagretEksternOppgave(
                oppgaveId = eksternOppgave.oppgaveId,
                sakId = eksternOppgave.sakId,
                opprettet = eksternOppgave.opprettet,
                grunnlag = eksternOppgave.grunnlag.toDbJson(),
                tilleggstekst = eksternOppgave.tilleggstekst,
            ),
        )
    }

    override fun hentForSakId(sakId: SakId): List<LagretEksternOppgave> =
        data.values.filter { it.sakId == sakId }
            .sortedWith(compareBy<LagretEksternOppgave> { it.opprettet }.thenBy { it.oppgaveId.toString() })
}
