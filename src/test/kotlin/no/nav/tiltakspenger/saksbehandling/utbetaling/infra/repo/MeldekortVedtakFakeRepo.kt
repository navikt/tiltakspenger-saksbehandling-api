@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk.Companion.opprett
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtaksliste
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MeldekortVedtakFakeRepo : MeldekortVedtakRepo {
    private val data = Atomic(mutableMapOf<VedtakId, MeldekortVedtak>())

    override fun lagre(vedtak: MeldekortVedtak, context: TransactionContext?) {
        data.get()[vedtak.id] = vedtak
    }

    override fun markerSendtTilUtbetaling(
        vedtakId: VedtakId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    ) {
        data.get()[vedtakId] = data.get()[vedtakId]!!.copy(sendtTilUtbetaling = tidspunkt)
    }

    override fun lagreFeilResponsFraUtbetaling(vedtakId: VedtakId, utbetalingsrespons: KunneIkkeUtbetale) {
        data.get()[vedtakId] = data.get()[vedtakId]!!.copy(sendtTilUtbetaling = null)
    }

    override fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        data.get()[vedtakId] =
            data.get()[vedtakId]!!.copy(journalpostId = journalpostId, journalføringstidspunkt = tidspunkt)
    }

    override fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String {
        return "fake-utbetaling-json"
    }

    fun hentForSakId(
        sakId: SakId,
    ): MeldekortVedtaksliste {
        return MeldekortVedtaksliste(data.get().values.filter { it.sakId == sakId })
    }

    override fun hentUtbetalingsvedtakForUtsjekk(limit: Int): List<MeldekortVedtak> {
        return data.get().values.filter { it.utbetaling.sendtTilUtbetaling == null }.take(limit)
    }

    override fun hentDeSomSkalJournalføres(limit: Int): List<MeldekortVedtak> {
        return data.get().values.filter { it.journalpostId == null }.take(limit)
    }

    override fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        data.get()[vedtakId] =
            data.get()[vedtakId]!!.copy(status = status)
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        return data.get()!!.filter {
            it.value.utbetaling.status in listOf(
                null,
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
            )
        }.map {
            UtbetalingDetSkalHentesStatusFor(
                sakId = it.value.sakId,
                saksnummer = it.value.saksnummer,
                vedtakId = it.value.id,
                opprettet = it.value.opprettet,
                sendtTilUtbetalingstidspunkt = it.value.utbetaling.sendtTilUtbetaling!!,
                forsøkshistorikk = Forsøkshistorikk(
                    forrigeForsøk = it.value.utbetaling.sendtTilUtbetaling!!.plus(1, ChronoUnit.MICROS),
                sendtTilUtbetalingstidspunkt = it.value.sendtTilUtbetaling!!,
                forsøkshistorikk = opprett(
                    forrigeForsøk = it.value.sendtTilUtbetaling!!.plus(1, ChronoUnit.MICROS),
                    antallForsøk = 1,
                    clock = fixedClock,
                ),
            )
        }
    }
}
