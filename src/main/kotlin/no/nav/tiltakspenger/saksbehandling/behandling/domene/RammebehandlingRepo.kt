package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.Saksbehandler
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

    fun lagreHvisFortsattUnderBeslutning(
        rammebehandling: Rammebehandling,
        utøvendeBeslutter: Saksbehandler,
        transactionContext: TransactionContext?,
    ): Boolean

    fun angreBehandling(
        rammebehandling: Rammebehandling,
        forventetSendtTilBeslutning: LocalDateTime?,
        transactionContext: TransactionContext?,
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

    /**
     * Lagrer at en saksbehandler har tatt over en automatisk behandlet søknadsbehandling som er klar til beslutning.
     * Hele raden skrives fra [rammebehandling], slik at nullstilt resultat og flagg for automatisk saksbehandling også lagres.
     * Lagres bare hvis raden fortsatt er klar til beslutning, tildelt den automatiske saksbehandleren, ikke satt på vent og er den samme innsendingen som ble lastet.
     *
     * @return true dersom behandlingen ble lagret, false dersom den er endret siden den ble lastet.
     */
    fun hentTilbake(
        rammebehandling: Rammebehandling,
        forventetSendtTilBeslutning: LocalDateTime?,
        transactionContext: TransactionContext?,
    ): Boolean

    /** Åpne automatiske søknadsbehandlinger som ikke venter (venter_til er null eller passert). */
    fun hentAutomatiskeSoknadsbehandlingIder(limit: Int): List<RammebehandlingId>

    /** Revurderinger under automatisk behandling som ikke venter (venter_til er null eller passert), eldste først. */
    fun hentAutomatiskeRevurderingIder(limit: Int): List<RammebehandlingId>
}
