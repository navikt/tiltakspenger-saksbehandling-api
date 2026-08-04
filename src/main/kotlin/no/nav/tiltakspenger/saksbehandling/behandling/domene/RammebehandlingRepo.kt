package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import java.time.LocalDateTime

interface RammebehandlingRepo {
    fun lagre(
        behandling: Rammebehandling,
        transactionContext: TransactionContext? = null,
    )

    fun oppdaterSimuleringMetadata(
        behandlingId: RammebehandlingId,
        originalResponseBody: String?,
        sessionContext: SessionContext,
    )

    fun hent(
        behandlingId: RammebehandlingId,
        sessionContext: SessionContext? = null,
    ): Rammebehandling

    fun hentBehandlingerTilDatadeling(limit: Int = 10): List<Rammebehandling>

    fun markerSendtTilDatadeling(id: RammebehandlingId, tidspunkt: LocalDateTime)

    fun taBehandlingSaksbehandler(
        rammebehandling: Rammebehandling,
        transactionContext: TransactionContext?,
    ): Boolean

    fun taBehandlingBeslutter(
        rammebehandling: Rammebehandling,
        sessionContext: SessionContext?,
    ): Boolean

    fun overtaSaksbehandler(
        rammebehandling: Rammebehandling,
        nåværendeSaksbehandler: String,
        transactionContext: TransactionContext?,
    ): Boolean

    fun overtaBeslutter(
        rammebehandling: Rammebehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean

    /** Åpne automatiske søknadsbehandlinger som ikke venter (venter_til er null eller passert). */
    fun hentAutomatiskeSoknadsbehandlingIder(limit: Int): List<RammebehandlingId>
}
