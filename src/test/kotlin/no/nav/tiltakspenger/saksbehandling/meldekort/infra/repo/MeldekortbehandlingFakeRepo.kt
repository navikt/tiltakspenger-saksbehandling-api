@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.LocalDateTime

/**
 * [SimuleringMedMetadata] blir ikke lagret siden den kun brukes for debug (hentes ikke opp fra basen igjen).
 */
class MeldekortbehandlingFakeRepo : MeldekortbehandlingRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, Meldekortbehandling>())
    val alle get() = data.get().values.toList()

    override fun lagre(
        meldekortbehandling: Meldekortbehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext?,
    ) {
        data.get()[meldekortbehandling.id] = meldekortbehandling
    }

    override fun oppdater(
        meldekortbehandling: Meldekortbehandling,
        transactionContext: TransactionContext?,
    ) {
        lagre(meldekortbehandling, null, transactionContext)
    }

    override fun oppdater(
        meldekortbehandling: Meldekortbehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext?,
    ) {
        lagre(meldekortbehandling, null, transactionContext)
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): Meldekortbehandlinger? =
        data
            .get()
            .values
            .filter { it.sakId == sakId }
            .sortedBy { it.opprettet }
            .let { meldekort ->
                meldekort.firstOrNull()?.let {
                    Meldekortbehandlinger(meldekort)
                }
            }

    override fun hent(meldekortId: MeldekortId, sessionContext: SessionContext?): Meldekortbehandling? {
        return data.get()[meldekortId]
    }

    override fun overtaSaksbehandler(
        meldekortId: MeldekortId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortbehandling = data.get()[meldekortId]
        require(meldekortbehandling != null && meldekortbehandling.saksbehandler == nåværendeSaksbehandler) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        if (meldekortbehandling is MeldekortUnderBehandling) {
            data.get()[meldekortId] = meldekortbehandling.copy(
                saksbehandler = nySaksbehandler.navIdent,
                sistEndret = sistEndret,
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
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortbehandling = data.get()[meldekortId]
        require(meldekortbehandling != null && meldekortbehandling.beslutter == nåværendeBeslutter) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke beslutter $nåværendeBeslutter"
        }
        if (meldekortbehandling is MeldekortbehandlingManuell) {
            data.get()[meldekortId] = meldekortbehandling.copy(
                beslutter = nyBeslutter.navIdent,
                sistEndret = sistEndret,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre beslutter for meldekort som ikke er manuelt behandlet")
        }
    }

    override fun taBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortbehandling = data.get()[meldekortId]
        require(meldekortbehandling != null && meldekortbehandling.saksbehandler == null && meldekortbehandling.status == MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke status KLAR_TIL_BEHANDLING eller har allerede saksbehandler"
        }
        if (meldekortbehandling is MeldekortUnderBehandling) {
            data.get()[meldekortId] = meldekortbehandling.copy(
                saksbehandler = saksbehandler.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
                sistEndret = sistEndret,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre saksbehandler for meldekort som ikke er under behandling")
        }
    }

    override fun taBehandlingBeslutter(
        meldekortId: MeldekortId,
        beslutter: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortbehandling = data.get()[meldekortId]
        require(meldekortbehandling != null && meldekortbehandling.beslutter == null && meldekortbehandling.status == MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke status KLAR_TIL_BESLUTNING eller har allerede beslutter"
        }
        if (meldekortbehandling is MeldekortbehandlingManuell) {
            data.get()[meldekortId] = meldekortbehandling.copy(
                beslutter = beslutter.navIdent,
                status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
                sistEndret = sistEndret,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre beslutter for meldekort som ikke er manuelt behandlet")
        }
    }

    override fun leggTilbakeBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        nåværendeSaksbehandler: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortbehandling = data.get()[meldekortId]
        require(meldekortbehandling != null && meldekortbehandling.saksbehandler == nåværendeSaksbehandler.navIdent && meldekortbehandling.status == MeldekortbehandlingStatus.UNDER_BEHANDLING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke saksbehandler $nåværendeSaksbehandler"
        }
        if (meldekortbehandling is MeldekortUnderBehandling) {
            data.get()[meldekortId] = meldekortbehandling.copy(
                saksbehandler = null,
                status = MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
                sistEndret = sistEndret,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke fjerne saksbehandler for meldekort som ikke er under behandling")
        }
    }

    override fun leggTilbakeBehandlingBeslutter(
        meldekortId: MeldekortId,
        nåværendeBeslutter: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        val meldekortbehandling = data.get()[meldekortId]
        require(meldekortbehandling != null && meldekortbehandling.beslutter == nåværendeBeslutter.navIdent && meldekortbehandling.status == MeldekortbehandlingStatus.UNDER_BESLUTNING) {
            "Meldekortbehandling med id $meldekortId finnes ikke eller har ikke beslutter $nåværendeBeslutter"
        }
        if (meldekortbehandling is MeldekortbehandlingManuell) {
            data.get()[meldekortId] = meldekortbehandling.copy(
                beslutter = null,
                status = MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
                sistEndret = sistEndret,
            )
            return true
        } else {
            throw IllegalStateException("Kan ikke endre beslutter for meldekort som ikke er manuelt behandlet")
        }
    }

    override fun hentBehandlingerTilDatadeling(limit: Int): List<Meldekortbehandling> {
        return emptyList()
    }

    override fun markerBehandlingSendtTilDatadeling(meldekortId: MeldekortId, tidspunkt: LocalDateTime) {
    }

    fun hentFnrForMeldekortId(
        meldekortId: MeldekortId,
    ): Fnr? = data.get()[meldekortId]?.fnr
}
