package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
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

    fun hentAlleForFnr(fnr: Fnr): List<Rammebehandling>

    fun hentBehandlingerTilDatadeling(limit: Int = 10): List<Rammebehandling>

    fun markerSendtTilDatadeling(id: RammebehandlingId, tidspunkt: LocalDateTime)

    fun taBehandlingSaksbehandler(
        rammebehandling: Rammebehandling,
        transactionContext: TransactionContext? = null,
    ): Boolean

    fun taBehandlingBeslutter(
        rammebehandling: Rammebehandling,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaSaksbehandler(
        rammebehandling: Rammebehandling,
        nåværendeSaksbehandler: String,
        transactionContext: TransactionContext? = null,
    ): Boolean

    fun overtaBeslutter(
        rammebehandling: Rammebehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun hentAlleAutomatiskeSoknadsbehandlinger(limit: Int): List<Søknadsbehandling>
}
