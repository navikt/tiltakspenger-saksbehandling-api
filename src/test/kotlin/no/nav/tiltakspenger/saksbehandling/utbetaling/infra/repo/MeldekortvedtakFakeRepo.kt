@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import java.time.LocalDateTime

class MeldekortvedtakFakeRepo(val utbetalingRepo: UtbetalingFakeRepo) : MeldekortvedtakRepo {
    private val data = Atomic(mutableMapOf<VedtakId, Meldekortvedtak>())

    override fun lagre(vedtak: Meldekortvedtak, context: TransactionContext?) {
        data.get()[vedtak.id] = vedtak
        utbetalingRepo.lagre(vedtak.utbetaling, context)
    }

    override fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        data.get()[vedtakId] =
            data.get()[vedtakId]!!.copy(journalpostId = journalpostId, journalføringstidspunkt = tidspunkt)
    }

    fun hentForSakId(
        sakId: SakId,
    ): Meldekortvedtaksliste {
        return Meldekortvedtaksliste(data.get().values.filter { it.sakId == sakId })
    }

    override fun hentDeSomSkalJournalføres(limit: Int): List<Meldekortvedtak> {
        return data.get().values.filter { it.journalpostId == null }.take(limit)
    }
}
