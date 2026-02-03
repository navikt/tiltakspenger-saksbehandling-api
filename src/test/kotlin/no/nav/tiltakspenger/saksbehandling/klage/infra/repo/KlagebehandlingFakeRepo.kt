package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo

class KlagebehandlingFakeRepo : KlagebehandlingRepo {

    private val data = Atomic(mutableMapOf<KlagebehandlingId, Klagebehandling>())
    val alle get() = data.get().values.toList()

    override fun lagreKlagebehandling(
        klagebehandling: Klagebehandling,
        sessionContext: SessionContext?,
    ) {
        data.get()[klagebehandling.id] = klagebehandling
    }

    override fun hentForRammebehandlingId(rammebehandlingId: BehandlingId): Klagebehandling? {
        return data.get().values.singleOrNullOrThrow { it.rammebehandlingId == rammebehandlingId }
    }

    override fun taBehandling(
        klagebehandling: Klagebehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandlingId = klagebehandling.id

        val behandling = data.get()[behandlingId]
        requireNotNull(behandling) {
            "Behandling med id $behandlingId finnes ikke"
        }
        require(behandling.saksbehandler == null) {
            "Saksbehandler ${behandling.saksbehandler} er ikke null"
        }
        data.get()[behandlingId] = klagebehandling
        return true
    }

    override fun overtaBehandling(
        klagebehandling: Klagebehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandlingId = klagebehandling.id
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.saksbehandler == nåværendeSaksbehandler) {
            "Behandling med id $behandlingId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        data.get()[behandlingId] = klagebehandling
        return true
    }

    fun hentForSakId(sakId: SakId): Klagebehandlinger {
        return Klagebehandlinger(data.get().values.filter { it.sakId == sakId })
    }
}
