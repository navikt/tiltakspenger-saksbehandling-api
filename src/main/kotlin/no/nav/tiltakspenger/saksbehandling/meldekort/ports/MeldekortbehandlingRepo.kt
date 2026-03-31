package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.LocalDateTime

interface MeldekortbehandlingRepo {

    /**
     * @param simuleringMedMetadata Vi tar inn denne separat, slik at vi slipper ha metadata i Meldekortbehandling (domenemodellen). Det er kun den "tolkede" simuleringen som hentes ut igjen, mens metadataen hentes ikke ut igjen; kun tenkt brukt til debug/notoritet.
     */
    fun lagre(
        meldekortbehandling: Meldekortbehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext? = null,
    )

    /** Oppdaterer ikke [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering] eller [SimuleringMedMetadata] */
    fun oppdater(
        meldekortbehandling: Meldekortbehandling,
        transactionContext: TransactionContext? = null,
    )

    /** Oppdaterer [SimuleringMedMetadata] eller nuller den ut */
    fun oppdater(
        meldekortbehandling: Meldekortbehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext? = null,
    )

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): Meldekortbehandlinger?

    fun hent(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): Meldekortbehandling?

    fun overtaSaksbehandler(
        meldekortId: MeldekortId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaBeslutter(
        meldekortId: MeldekortId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun taBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun taBehandlingBeslutter(
        meldekortId: MeldekortId,
        beslutter: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        nåværendeSaksbehandler: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingBeslutter(
        meldekortId: MeldekortId,
        nåværendeBeslutter: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun hentBehandlingerTilDatadeling(limit: Int = 10): List<Meldekortbehandling>

    fun markerBehandlingSendtTilDatadeling(meldekortId: MeldekortId, tidspunkt: LocalDateTime)
}
