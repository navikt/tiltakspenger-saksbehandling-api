package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import java.time.LocalDateTime

class BehandlingFakeRepo : BehandlingRepo {
    private val data = Atomic(mutableMapOf<BehandlingId, Behandling>())

    val alle get() = data.get().values.toList()

    override fun lagre(
        behandling: Behandling,
        transactionContext: TransactionContext?,
    ) {
        data.get()[behandling.id] = behandling
    }

    override fun lagre(
        behandling: Søknadsbehandling,
        transactionContext: TransactionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentOrNull(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling? {
        return data.get()[behandlingId]
    }

    override fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling {
        return hentOrNull(behandlingId, sessionContext)!!
    }

    override fun hentAlleForFnr(fnr: Fnr): List<Behandling> = data.get().values.filter { it.fnr == fnr }

    override fun hentForSøknadId(søknadId: SøknadId): Behandling? = data.get().values.find { it.søknad?.id == søknadId }

    override fun hentFørstegangsbehandlingerTilDatadeling(limit: Int): List<Behandling> {
        return data.get().values.filter {
            it.sendtTilDatadeling == null
        }
    }

    override fun markerSendtTilDatadeling(id: BehandlingId, tidspunkt: LocalDateTime) {
        data.get()[id] = data.get()[id]!!.copy(
            sendtTilDatadeling = tidspunkt,
        )
    }

    override fun taBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.saksbehandler == null) {
            "Behandling med id $behandlingId finnes ikke eller beslutter ${behandling?.saksbehandler} er ikke null"
        }
        data.get()[behandlingId] = data.get()[behandlingId]!!.copy(
            saksbehandler = saksbehandler.navIdent,
            status = behandlingsstatus,
        )
        return true
    }

    override fun taBehandlingBeslutter(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.beslutter == null) {
            "Behandling med id $behandlingId finnes ikke eller beslutter ${behandling?.beslutter} er ikke null"
        }
        data.get()[behandlingId] = data.get()[behandlingId]!!.copy(
            beslutter = beslutter.navIdent,
            status = behandlingsstatus,
        )
        return true
    }

    override fun overtaSaksbehandler(
        behandlingId: BehandlingId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.saksbehandler == nåværendeSaksbehandler) {
            "Behandling med id $behandlingId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        data.get()[behandlingId] = data.get()[behandlingId]!!.copy(
            saksbehandler = nySaksbehandler.navIdent,
        )
        return true
    }

    override fun overtaBeslutter(
        behandlingId: BehandlingId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.beslutter == nåværendeBeslutter) {
            "Behandling med id $behandlingId finnes ikke eller har ikke beslutter $nåværendeBeslutter"
        }
        data.get()[behandlingId] = data.get()[behandlingId]!!.copy(
            beslutter = nyBeslutter.navIdent,
        )
        return true
    }

    override fun leggTilbakeBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        nåværendeSaksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.saksbehandler == nåværendeSaksbehandler.navIdent) {
            "Behandling med id $behandlingId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        data.get()[behandlingId] = data.get()[behandlingId]!!.copy(
            saksbehandler = null,
            status = behandlingsstatus,
        )
        return true
    }

    override fun leggTilbakeBehandlingBeslutter(
        behandlingId: BehandlingId,
        nåværendeBeslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        require(behandling != null && behandling.beslutter == nåværendeBeslutter.navIdent) {
            "Behandling med id $behandlingId finnes ikke eller har ikke beslutter $nåværendeBeslutter"
        }
        data.get()[behandlingId] = data.get()[behandlingId]!!.copy(
            beslutter = null,
            status = behandlingsstatus,
        )
        return true
    }

    fun hentBehandlingerForSakId(sakId: SakId): Behandlinger {
        return Behandlinger(
            data.get().values.filter { it.sakId == sakId },
        )
    }
}
