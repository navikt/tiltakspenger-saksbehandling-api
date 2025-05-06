package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata

interface MeldekortBehandlingRepo {

    /**
     * @param simuleringMedMetadata Vi tar inn denne separat, slik at vi slipper ha metadata i MeldekortBehandling (domenemodellen). Det er kun den "tolkede" simuleringen som hentes ut igjen, mens metadataen hentes ikke ut igjen; kun tenkt brukt til debug/notoritet.
     */
    fun lagre(
        meldekortBehandling: MeldekortBehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext? = null,
    )

    /** Oppdaterer ikke [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering] eller [SimuleringMedMetadata] */
    fun oppdater(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext? = null,
    )

    /** Oppdaterer [SimuleringMedMetadata] eller nuller den ut */
    fun oppdater(
        meldekortBehandling: MeldekortBehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext? = null,
    )

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): MeldekortBehandlinger?

    fun hent(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): MeldekortBehandling?

    fun overtaSaksbehandler(
        meldekortId: MeldekortId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun overtaBeslutter(
        meldekortId: MeldekortId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun taBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun taBehandlingBeslutter(
        meldekortId: MeldekortId,
        beslutter: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        nåværendeSaksbehandler: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun leggTilbakeBehandlingBeslutter(
        meldekortId: MeldekortId,
        nåværendeBeslutter: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext? = null,
    ): Boolean
}
