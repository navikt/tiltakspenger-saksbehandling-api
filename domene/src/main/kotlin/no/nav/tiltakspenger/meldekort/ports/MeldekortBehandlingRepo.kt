package no.nav.tiltakspenger.meldekort.ports

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger

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
}
