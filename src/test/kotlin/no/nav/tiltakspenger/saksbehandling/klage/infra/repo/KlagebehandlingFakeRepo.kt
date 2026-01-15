package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo

class KlagebehandlingFakeRepo : KlagebehandlingRepo {

    private val data = Atomic(mutableMapOf<KlagebehandlingId, Klagebehandling>())
    val alle get() = data.get().values.toList()

    override fun lagreKlagebehandling(
        klagebehandling: Klagebehandling,
        transactionContext: TransactionContext?,
    ) {
        data.get()[klagebehandling.id] = klagebehandling
    }

    fun hentForSakId(sakId: SakId): Klagebehandlinger {
        return Klagebehandlinger(data.get().values.filter { it.sakId == sakId })
    }
}
