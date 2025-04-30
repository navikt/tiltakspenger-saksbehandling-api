package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger

interface MeldekortBehandlingRepo {

    fun lagre(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext? = null,
    )

    /**
     * TODO jah: Slå sammen lagre og oppdater til en metode.
     */
    fun oppdater(
        meldekortBehandling: MeldekortBehandling,
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
