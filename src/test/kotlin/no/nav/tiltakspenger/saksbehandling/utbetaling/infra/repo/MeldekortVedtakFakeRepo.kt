@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtaksliste
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import java.time.LocalDateTime

class MeldekortVedtakFakeRepo : MeldekortVedtakRepo {
    private val data = Atomic(mutableMapOf<VedtakId, MeldekortVedtak>())

    override fun opprett(vedtak: MeldekortVedtak, context: TransactionContext?) {
        data.get()[vedtak.id] = vedtak
    }

//    override fun markerSendtTilUtbetaling(
//        vedtakId: VedtakId,
//        tidspunkt: LocalDateTime,
//        utbetalingsrespons: SendtUtbetaling,
//    ) {
//        data.get()[vedtakId] = data.get()[vedtakId]!!.copy(sendtTilUtbetaling = tidspunkt)
//    }
//
//    override fun lagreFeilResponsFraUtbetaling(vedtakId: VedtakId, utbetalingsrespons: KunneIkkeUtbetale) {
//        data.get()[vedtakId] = data.get()[vedtakId]!!.copy(sendtTilUtbetaling = null)
//    }

    override fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        data.get()[vedtakId] =
            data.get()[vedtakId]!!.copy(journalpostId = journalpostId, journalføringstidspunkt = tidspunkt)
    }

//    override fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String {
//        return "fake-utbetaling-json"
//    }

    fun hentForSakId(
        sakId: SakId,
    ): MeldekortVedtaksliste {
        return MeldekortVedtaksliste(data.get().values.filter { it.sakId == sakId })
    }

//    override fun hentUtbetalingsvedtakForUtsjekk(limit: Int): List<MeldekortVedtak> {
//        return data.get().values.filter { it.utbetaling.sendtTilUtbetaling == null }.take(limit)
//    }

    override fun hentDeSomSkalJournalføres(limit: Int): List<MeldekortVedtak> {
        return data.get().values.filter { it.journalpostId == null }.take(limit)
    }

//    override fun oppdaterUtbetalingsstatus(
//        vedtakId: VedtakId,
//        status: Utbetalingsstatus,
//        metadata: Forsøkshistorikk,
//        context: TransactionContext?,
//    ) {
//        data.get()[vedtakId] =
//            data.get()[vedtakId]!!.copy(status = status)
//    }

//    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
//        return data.get()!!.filter {
//            it.value.utbetaling.status in listOf(
//                null,
//                Utbetalingsstatus.IkkePåbegynt,
//                Utbetalingsstatus.SendtTilOppdrag,
//            )
//        }.map {
//            UtbetalingDetSkalHentesStatusFor(
//                sakId = it.value.sakId,
//                saksnummer = it.value.saksnummer,
//                vedtakId = it.value.id,
//                opprettet = it.value.opprettet,
//                sendtTilUtbetalingstidspunkt = it.value.utbetaling.sendtTilUtbetaling!!,
//                forsøkshistorikk = opprett(
//                    forrigeForsøk = it.value.utbetaling.sendtTilUtbetaling!!.plus(1, ChronoUnit.MICROS),
//                    antallForsøk = 1,
//                    clock = fixedClock,
//                ),
//            )
//        }
//    }
}
