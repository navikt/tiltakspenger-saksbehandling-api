package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.vedtak.meldekort.ports.MeldekortBehandlingRepo

class MeldekortBehandlingFakeRepo : MeldekortBehandlingRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, MeldekortBehandling>())

    override fun lagre(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        data.get()[meldekortBehandling.id] = meldekortBehandling
    }

    override fun oppdater(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        lagre(meldekortBehandling, transactionContext)
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): MeldekortBehandlinger? =
        data
            .get()
            .values
            .filter { it.sakId == sakId }
            .sortedBy { it.opprettet }
            .let { meldekort ->
                meldekort.firstOrNull()?.let {
                    MeldekortBehandlinger(meldekort)
                }
            }

    fun hentFnrForMeldekortId(
        meldekortId: MeldekortId,
    ): Fnr? = data.get()[meldekortId]?.fnr
}
