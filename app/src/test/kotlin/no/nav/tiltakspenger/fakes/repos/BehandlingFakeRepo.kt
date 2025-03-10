package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.BehandlingRepo
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

    override fun hentOrNull(
        behandlingId: BehandlingId,
        sessionContext: no.nav.tiltakspenger.libs.persistering.domene.SessionContext?,
    ): Behandling? {
        return data.get()[behandlingId]
    }

    override fun hent(
        behandlingId: BehandlingId,
        sessionContext: no.nav.tiltakspenger.libs.persistering.domene.SessionContext?,
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

    fun hentBehandlingerForSakId(sakId: SakId): Behandlinger {
        return Behandlinger(
            data.get().values.filter { it.sakId == sakId },
        )
    }
}
