package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktstatus
import java.util.concurrent.CopyOnWriteArrayList

class UtbetalingsoversiktFakeRepo : UtbetalingsoversiktRepo {
    private val oversikter = CopyOnWriteArrayList<Utbetalingsoversikt>()

    override fun lagre(oversikt: Utbetalingsoversikt, metadata: UtbetalingsoversiktMetadata) {
        oversikter.add(oversikt)
    }

    override fun hentStatusForSak(sakId: SakId): Utbetalingsoversiktstatus {
        val forSak = oversikter.filter { it.sakId == sakId }.sortedWith(compareBy({ it.hentet }, { it.id.toString() }))
        return when (val siste = forSak.lastOrNull()) {
            null -> Utbetalingsoversiktstatus.IkkeHentet

            is Utbetalingsoversikt.Vellykket -> Utbetalingsoversiktstatus.SisteOppslagVellykket(siste)

            is Utbetalingsoversikt.Feilet -> Utbetalingsoversiktstatus.SisteOppslagFeilet(
                oversikt = siste,
                sisteVellykkede = forSak.filterIsInstance<Utbetalingsoversikt.Vellykket>().lastOrNull(),
            )
        }
    }
}
