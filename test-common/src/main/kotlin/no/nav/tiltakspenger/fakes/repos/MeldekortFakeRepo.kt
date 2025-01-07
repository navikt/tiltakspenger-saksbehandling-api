package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import java.time.LocalDateTime

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

    override fun hentUsendteTilBruker(): List<MeldekortBehandling> {
        TODO("Not yet implemented")
    }

    override fun markerSomSendtTilBruker(meldekortId: MeldekortId, tidspunkt: LocalDateTime) {
        TODO("Not yet implemented")
    }

    fun hentForSakId(
        sakId: SakId,
    ): MeldekortBehandlinger? =
        data
            .get()
            .values
            .filter { it.sakId == sakId }
            .let { meldekort ->
                meldekort.firstOrNull()?.let {
                    MeldekortBehandlinger(it.tiltakstype, meldekort)
                }
            }

    fun hentFnrForMeldekortId(
        meldekortId: MeldekortId,
    ): Fnr? = data.get()[meldekortId]?.fnr
}
