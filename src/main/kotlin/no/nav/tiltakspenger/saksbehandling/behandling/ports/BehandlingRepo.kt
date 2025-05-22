package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import java.time.LocalDateTime

interface BehandlingRepo {
    fun lagre(
        behandling: Behandling,
        transactionContext: TransactionContext? = null,
    )

    fun hentOrNull(
        behandlingId: BehandlingId,
        sessionContext: SessionContext? = null,
    ): Behandling?

    fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext? = null,
    ): Behandling

    fun hentAlleForFnr(fnr: Fnr): List<Behandling>

    fun hentForSøknadId(søknadId: SøknadId): Behandling?

    fun hentSøknadsbehandlingerTilDatadeling(limit: Int = 10): List<Behandling>

    fun markerSendtTilDatadeling(id: BehandlingId, tidspunkt: LocalDateTime)

    fun taBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun taBehandlingBeslutter(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaSaksbehandler(
        behandlingId: BehandlingId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaBeslutter(
        behandlingId: BehandlingId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        nåværendeSaksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingBeslutter(
        behandlingId: BehandlingId,
        nåværendeBeslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext? = null,
    ): Boolean
}
