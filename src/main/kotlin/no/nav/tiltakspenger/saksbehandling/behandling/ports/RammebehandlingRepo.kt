package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import java.time.LocalDateTime

interface RammebehandlingRepo {
    fun lagre(
        behandling: Rammebehandling,
        transactionContext: TransactionContext? = null,
    )

    fun oppdaterSimuleringMetadata(
        behandlingId: BehandlingId,
        originalResponseBody: String?,
        sessionContext: SessionContext,
    )

    fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext? = null,
    ): Rammebehandling

    fun hentAlleForFnr(fnr: Fnr): List<Rammebehandling>

    fun hentBehandlingerTilDatadeling(limit: Int = 10): List<Rammebehandling>

    fun markerSendtTilDatadeling(id: BehandlingId, tidspunkt: LocalDateTime)

    fun taBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun taBehandlingBeslutter(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaSaksbehandler(
        behandlingId: BehandlingId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaBeslutter(
        behandlingId: BehandlingId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        nåværendeSaksbehandler: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingBeslutter(
        behandlingId: BehandlingId,
        nåværendeBeslutter: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun hentAlleAutomatiskeSoknadsbehandlinger(limit: Int): List<Søknadsbehandling>
}
