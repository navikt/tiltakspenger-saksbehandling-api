package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.LocalDate
import java.time.LocalDateTime

interface KlagevedtakRepo {
    fun lagreVedtak(klagevedtak: Klagevedtak, sessionContext: SessionContext?)

    fun markerJournalført(
        id: VedtakId,
        vedtaksdato: LocalDate,
        metadata: JournalførBrevMetadata,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    )

    fun markerDistribuert(id: VedtakId, distribusjonId: DistribusjonId, distribusjonstidspunkt: LocalDateTime)

    fun hentKlagevedtakSomSkalJournalføres(limit: Int = 10): List<Klagevedtak>

    fun hentKlagevedtakSomSkalJournalføresForSakId(sakId: SakId): List<Klagevedtak>

    fun hentKlagevedtakSomSkalDistribueres(limit: Int = 10): List<VedtakSomSkalDistribueres>

    fun hentKlagevedtakSomSkalDistribueresForSakId(sakId: SakId): List<VedtakSomSkalDistribueres>
}
