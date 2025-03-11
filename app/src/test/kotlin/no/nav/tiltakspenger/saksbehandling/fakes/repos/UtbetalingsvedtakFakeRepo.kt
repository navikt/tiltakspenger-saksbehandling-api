package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import java.time.LocalDateTime

class UtbetalingsvedtakFakeRepo : UtbetalingsvedtakRepo {
    private val data = Atomic(mutableMapOf<VedtakId, Utbetalingsvedtak>())

    override fun lagre(vedtak: Utbetalingsvedtak, context: TransactionContext?) {
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
    ): Utbetalinger {
        return Utbetalinger(data.get().values.filter { it.sakId == sakId })
    }

    override fun hentUtbetalingsvedtakForUtsjekk(limit: Int): List<Utbetalingsvedtak> {
        return data.get().values.filter { it.sendtTilUtbetaling == null }.take(limit)
    }

    override fun hentDeSomSkalJournalføres(limit: Int): List<Utbetalingsvedtak> {
        return data.get().values.filter { it.journalpostId == null }.take(limit)
    }
}
