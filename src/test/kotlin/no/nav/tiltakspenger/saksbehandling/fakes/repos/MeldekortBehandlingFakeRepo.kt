package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata

/**
 * [SimuleringMedMetadata] blir ikke lagret siden den kun brukes for debug (hentes ikke opp fra basen igjen).
 */
class MeldekortBehandlingFakeRepo : MeldekortBehandlingRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, MeldekortBehandling>())

    override fun lagre(
        meldekortBehandling: MeldekortBehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext?,
    ) {
        data.get()[meldekortBehandling.id] = meldekortBehandling
    }

    override fun oppdater(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        lagre(meldekortBehandling, null, transactionContext)
    }

    override fun oppdater(
        meldekortBehandling: MeldekortBehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext?,
    ) {
        lagre(meldekortBehandling, null, transactionContext)
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

    override fun hent(meldekortId: MeldekortId, sessionContext: SessionContext?): MeldekortBehandling? {
        return data.get()[meldekortId]
    }

    override fun overtaSaksbehandler(
        meldekortId: MeldekortId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortBehandling = data.get()[meldekortId]
        require(meldekortBehandling != null && meldekortBehandling.saksbehandler == nåværendeSaksbehandler) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        if (meldekortBehandling is MeldekortUnderBehandling) {
            data.get()[meldekortId] = meldekortBehandling.copy(
                saksbehandler = nySaksbehandler.navIdent,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre saksbehandler for meldekort som ikke er under behandling")
        }
    }

    override fun overtaBeslutter(
        meldekortId: MeldekortId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortBehandling = data.get()[meldekortId]
        require(meldekortBehandling != null && meldekortBehandling.beslutter == nåværendeBeslutter) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke beslutter $nåværendeBeslutter"
        }
        if (meldekortBehandling is MeldekortBehandletManuelt) {
            data.get()[meldekortId] = meldekortBehandling.copy(
                beslutter = nyBeslutter.navIdent,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre beslutter for meldekort som ikke er manuelt behandlet")
        }
    }

    override fun taBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortBehandling = data.get()[meldekortId]
        require(meldekortBehandling != null && meldekortBehandling.saksbehandler == null && meldekortBehandling.status == MeldekortBehandlingStatus.UNDER_BEHANDLING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke status UNDER_BEHANDLING eller har allerede saksbehandler"
        }
        if (meldekortBehandling is MeldekortUnderBehandling) {
            data.get()[meldekortId] = meldekortBehandling.copy(
                saksbehandler = saksbehandler.navIdent,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre saksbehandler for meldekort som ikke er under behandling")
        }
    }

    override fun taBehandlingBeslutter(
        meldekortId: MeldekortId,
        beslutter: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortBehandling = data.get()[meldekortId]
        require(meldekortBehandling != null && meldekortBehandling.beslutter == null && meldekortBehandling.status == MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke status KLAR_TIL_BESLUTNING eller har allerede beslutter"
        }
        if (meldekortBehandling is MeldekortBehandletManuelt) {
            data.get()[meldekortId] = meldekortBehandling.copy(
                beslutter = beslutter.navIdent,
                status = MeldekortBehandlingStatus.UNDER_BESLUTNING,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre beslutter for meldekort som ikke er manuelt behandlet")
        }
    }

    override fun leggTilbakeBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        nåværendeSaksbehandler: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortBehandling = data.get()[meldekortId]
        require(meldekortBehandling != null && meldekortBehandling.saksbehandler == nåværendeSaksbehandler.navIdent && meldekortBehandling.status == MeldekortBehandlingStatus.UNDER_BEHANDLING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        if (meldekortBehandling is MeldekortUnderBehandling) {
            data.get()[meldekortId] = meldekortBehandling.copy(
                saksbehandler = null,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke fjerne saksbehandler for meldekort som ikke er under behandling")
        }
    }

    override fun leggTilbakeBehandlingBeslutter(
        meldekortId: MeldekortId,
        nåværendeBeslutter: Saksbehandler,
        meldekortBehandlingStatus: MeldekortBehandlingStatus,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortBehandling = data.get()[meldekortId]
        require(meldekortBehandling != null && meldekortBehandling.beslutter == nåværendeBeslutter.navIdent && meldekortBehandling.status == MeldekortBehandlingStatus.UNDER_BESLUTNING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke beslutter $nåværendeBeslutter"
        }
        if (meldekortBehandling is MeldekortBehandletManuelt) {
            data.get()[meldekortId] = meldekortBehandling.copy(
                beslutter = null,
                status = MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre beslutter for meldekort som ikke er manuelt behandlet")
        }
    }

    fun hentFnrForMeldekortId(
        meldekortId: MeldekortId,
    ): Fnr? = data.get()[meldekortId]?.fnr
}
