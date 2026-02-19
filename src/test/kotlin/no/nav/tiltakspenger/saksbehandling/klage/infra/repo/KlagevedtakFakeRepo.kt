package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.`journalføring`.`JournalførBrevMetadata`
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtaksliste
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.LocalDate
import java.time.LocalDateTime

class KlagevedtakFakeRepo : KlagevedtakRepo {
    private val data = Atomic(mutableMapOf<VedtakId, Klagevedtak>())
    val alle get() = data.get().values.toList()

    fun hentForSakId(sakId: SakId): Klagevedtaksliste {
        return data.get().values.toList()
            .filter { it.sakId == sakId }
            .let { Klagevedtaksliste(it) }
    }

    override fun lagreVedtak(
        klagevedtak: Klagevedtak,
        sessionContext: SessionContext?,
    ) {
        data.get()[klagevedtak.id] = klagevedtak
    }

    override fun markerJournalført(
        id: VedtakId,
        vedtaksdato: LocalDate,
        metadata: JournalførBrevMetadata,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        data.get()[id] =
            data.get()[id]!!.copy(
                journalpostId = journalpostId,
                journalføringstidspunkt = tidspunkt,
                vedtaksdato = vedtaksdato,
            )
    }

    override fun markerDistribuert(
        id: VedtakId,
        distribusjonId: DistribusjonId,
        distribusjonstidspunkt: LocalDateTime,
    ) {
        data.get()[id] =
            data.get()[id]!!.copy(
                distribusjonId = distribusjonId,
                distribusjonstidspunkt = distribusjonstidspunkt,
            )
    }

    override fun markerSendtTilDatadeling(
        id: VedtakId,
        tidspunkt: LocalDateTime,
    ) {
        data.get()[id] =
            data.get()[id]!!.copy(
                sendtTilDatadeling = tidspunkt,
            )
    }

    override fun hentKlagevedtakSomSkalJournalføres(limit: Int): List<Klagevedtak> {
        return data.get().values.filter { it.journalpostId == null }.sortedBy { it.opprettet }.take(limit)
    }

    override fun hentKlagevedtakSomSkalDistribueres(limit: Int): List<VedtakSomSkalDistribueres> {
        return data.get().values
            .filter { it.journalpostId != null && it.journalføringstidspunkt != null && it.distribusjonstidspunkt == null && it.distribusjonId == null }
            .sortedBy { it.journalføringstidspunkt }
            .take(limit)
            .map { VedtakSomSkalDistribueres(it.id, it.journalpostId!!) }
    }
}
