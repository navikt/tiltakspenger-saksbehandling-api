@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
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

    override fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling {
        return data.get()[behandlingId]!!
    }

    override fun hentAlleForFnr(fnr: Fnr): List<Behandling> = data.get().values.filter { it.fnr == fnr }

    override fun hentForSøknadId(søknadId: SøknadId): List<Søknadsbehandling> =
        data.get().values
            .filterIsInstance<Søknadsbehandling>()
            .filter { it.søknad.id == søknadId }

    override fun hentSøknadsbehandlingerTilDatadeling(limit: Int): List<Behandling> {
        return data.get().values.filter {
            it.sendtTilDatadeling == null
        }
    }

    override fun markerSendtTilDatadeling(id: BehandlingId, tidspunkt: LocalDateTime) {
        val behandling = data.get()[id]!!

        data.get()[id] = when (behandling) {
            is Revurdering -> behandling.copy(sendtTilDatadeling = tidspunkt)
            is Søknadsbehandling -> behandling.copy(sendtTilDatadeling = tidspunkt)
        }
    }

    override fun hentAlleAutomatiskeSoknadsbehandlinger(limit: Int): List<Søknadsbehandling> {
        return data.get().values.filter {
            it.status == Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        }.filterIsInstance<Søknadsbehandling>()
    }

    override fun taBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        requireNotNull(behandling) {
            "Behandling med id $behandlingId finnes ikke"
        }
        require(behandling.saksbehandler == null) {
            "Saksbehandler ${behandling.saksbehandler} er ikke null"
        }

        data.get()[behandlingId] = when (behandling) {
            is Revurdering -> behandling.copy(
                saksbehandler = saksbehandler.navIdent,
                status = behandlingsstatus,
            )

            is Søknadsbehandling -> behandling.copy(
                saksbehandler = saksbehandler.navIdent,
                status = behandlingsstatus,
            )
        }
        return true
    }

    override fun taBehandlingBeslutter(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val behandling = data.get()[behandlingId]
        requireNotNull(behandling) {
            "Behandling med id $behandlingId finnes ikke"
        }
        require(behandling.beslutter == null) {
            "Behandling ${behandling.saksbehandler} er ikke null"
        }

        data.get()[behandlingId] = when (behandling) {
            is Revurdering -> behandling.copy(
                beslutter = beslutter.navIdent,
                status = behandlingsstatus,
            )

            is Søknadsbehandling -> behandling.copy(
                beslutter = beslutter.navIdent,
                status = behandlingsstatus,
            )
        }

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

        data.get()[behandlingId] = when (behandling) {
            is Revurdering -> behandling.copy(
                saksbehandler = nySaksbehandler.navIdent,
            )

            is Søknadsbehandling -> behandling.copy(
                saksbehandler = nySaksbehandler.navIdent,
            )
        }
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

        data.get()[behandlingId] = when (behandling) {
            is Revurdering -> behandling.copy(
                beslutter = nyBeslutter.navIdent,
            )

            is Søknadsbehandling -> behandling.copy(
                beslutter = nyBeslutter.navIdent,
            )
        }
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

        data.get()[behandlingId] = when (behandling) {
            is Revurdering -> behandling.copy(
                saksbehandler = null,
                status = behandlingsstatus,
            )

            is Søknadsbehandling -> behandling.copy(
                saksbehandler = null,
                status = behandlingsstatus,
            )
        }
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

        data.get()[behandlingId] = when (behandling) {
            is Revurdering -> behandling.copy(
                beslutter = null,
                status = behandlingsstatus,
            )

            is Søknadsbehandling -> behandling.copy(
                beslutter = null,
                status = behandlingsstatus,
            )
        }
        return true
    }

    fun hentBehandlingerForSakId(sakId: SakId): Behandlinger {
        return Behandlinger(
            data.get().values.filter { it.sakId == sakId },
        )
    }
}
