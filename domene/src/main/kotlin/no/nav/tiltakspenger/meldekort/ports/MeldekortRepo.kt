package no.nav.tiltakspenger.meldekort.ports

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling

interface MeldekortRepo {

    fun lagre(
        meldekort: MeldekortBehandling,
        transactionContext: TransactionContext? = null,
    )

    /**
     * TODO jah: Slå sammen lagre og oppdater til en metode.
     */
    fun oppdater(
        meldekort: MeldekortBehandling,
        transactionContext: TransactionContext? = null,
    )
}
