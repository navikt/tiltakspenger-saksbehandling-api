package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo

class MeldekortFakeRepo : MeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, MeldekortBehandling>())

    override fun lagre(
        meldekort: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        data.get()[meldekort.id] = meldekort
    }

    override fun oppdater(
        meldekort: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        lagre(meldekort, transactionContext)
    }

    override fun hentForMeldekortId(meldekortId: MeldekortId, sessionContext: SessionContext?): MeldekortBehandling? {
        return data.get()[meldekortId]
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): MeldekortBehandlinger? =
        data
            .get()
            .values
            .filter { it.sakId == sakId }
            .sortedBy { it.opprettet }
            .let { meldekort ->
                meldekort.firstOrNull()?.let {
                    MeldekortBehandlinger(it.tiltakstype, meldekort)
                }
            }

    fun hentFnrForMeldekortId(
        meldekortId: MeldekortId,
    ): Fnr? = data.get()[meldekortId]?.fnr
}
