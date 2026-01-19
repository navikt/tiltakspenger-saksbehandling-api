package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.LocalDate
import java.time.LocalDateTime

interface KlagevedtakRepo {
    fun lagreVedtak(klagevedtak: Klagevedtak, sessionContext: SessionContext? = null)
    fun markerJournalført(
        id: VedtakId,
        vedtaksdato: LocalDate,
        brevJson: String,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    )

    fun markerDistribuert(id: VedtakId, distribusjonId: DistribusjonId, distribusjonstidspunkt: LocalDateTime)
    fun markerSendtTilDatadeling(id: VedtakId, tidspunkt: LocalDateTime)
    fun hentKlagevedtakSomSkalJournalføres(limit: Int = 10): List<Klagevedtak>
    fun hentKlagevedtakSomSkalDistribueres(limit: Int = 10): List<VedtakSomSkalDistribueres>
}
