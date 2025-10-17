@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingFakeRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.LocalDate
import java.time.LocalDateTime

class RammevedtakFakeRepo(val utbetalingRepo: UtbetalingFakeRepo) : RammevedtakRepo {

    private val data = Atomic(mutableMapOf<VedtakId, Rammevedtak>())

    override fun hentForVedtakId(vedtakId: VedtakId): Rammevedtak? = data.get()[vedtakId]

    override fun lagre(
        vedtak: Rammevedtak,
        context: TransactionContext?,
    ) {
        data.get()[vedtak.id] = vedtak
        vedtak.utbetaling?.also {
            utbetalingRepo.lagre(it, context)
        }
    }

    override fun hentForFnr(fnr: Fnr): List<Rammevedtak> =
        data.get().values.filter { it.behandling.fnr == fnr }.sortedBy { it.opprettet }

    override fun hentRammevedtakSomSkalJournalføres(limit: Int): List<Rammevedtak> {
        return data.get().values.filter { it.journalpostId == null }.sortedBy { it.opprettet }.take(limit)
    }

    override fun markerJournalført(
        id: VedtakId,
        vedtaksdato: LocalDate,
        brevJson: String,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        data.get()[id] = data.get()[id]!!.copy(journalpostId = journalpostId, journalføringstidspunkt = tidspunkt)
    }

    override fun hentRammevedtakSomSkalDistribueres(limit: Int): List<VedtakSomSkalDistribueres> {
        return data.get().values.filter { it.distribusjonId == null }.map {
            VedtakSomSkalDistribueres(it.id, it.journalpostId!!)
        }.take(limit)
    }

    override fun markerDistribuert(id: VedtakId, distribusjonId: DistribusjonId, tidspunkt: LocalDateTime) {
        data.get()[id] = data.get()[id]!!.copy(distribusjonId = distribusjonId, distribusjonstidspunkt = tidspunkt)
    }

    override fun hentRammevedtakTilDatadeling(limit: Int): List<Rammevedtak> {
        return data.get().values.filter { it.sendtTilDatadeling == null }.sortedBy { it.opprettet }.take(limit)
    }

    override fun markerSendtTilDatadeling(id: VedtakId, tidspunkt: LocalDateTime) {
        data.get()[id] = data.get()[id]!!.copy(sendtTilDatadeling = tidspunkt)
    }

    override fun markerOmgjortAv(
        vedtakId: VedtakId,
        omgjortAvRammevedtakId: VedtakId,
    ) {
        data.get()[vedtakId] = data.get()[vedtakId]!!.copy(omgjortAvRammevedtakId = omgjortAvRammevedtakId)
    }

    fun hentForSakId(sakId: SakId): Rammevedtaksliste =
        data.get().values.filter { it.behandling.sakId == sakId }.sortedBy { it.opprettet }.let {
            Rammevedtaksliste(it)
        }

    fun hentForBehandlingId(behandlingId: BehandlingId): Rammevedtak? =
        data.get().values.find { it.behandling.id == behandlingId }
}
