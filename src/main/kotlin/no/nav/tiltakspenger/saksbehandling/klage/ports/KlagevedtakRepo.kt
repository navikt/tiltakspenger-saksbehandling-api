package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
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
        metadata: JournalførBrevMetadata,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    )

    fun markerDistribuert(id: VedtakId, distribusjonId: DistribusjonId, distribusjonstidspunkt: LocalDateTime)

    /**
     * **Kalles ikke fra noe sted i dag (per 2026-08-02).**
     * [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService] deler sak, rammevedtak, rammebehandling og meldekortvedtak — ikke klagevedtak.
     * `tiltakspenger-datadeling` kjenner ikke klagebegrepet i det hele tatt, så det finnes ingen mottaker å sende til.
     *
     * Kolonnen `sendt_til_datadeling` skrives ved [lagreVedtak] og leses tilbake til [Klagevedtak.sendtTilDatadeling], men står alltid som null.
     * Vi fant ingen issue eller nedskrevet avklaring på om klagevedtak *skal* deles.
     * Ta stilling til det før du bruker denne: enten kobles klagevedtak på datadelingen, eller så ryddes metoden, feltet og kolonnen bort sammen.
     */
    fun markerSendtTilDatadeling(id: VedtakId, tidspunkt: LocalDateTime)

    fun hentKlagevedtakSomSkalJournalføres(limit: Int = 10): List<Klagevedtak>

    fun hentKlagevedtakSomSkalJournalføresForSakId(sakId: SakId): List<Klagevedtak>

    fun hentKlagevedtakSomSkalDistribueres(limit: Int = 10): List<VedtakSomSkalDistribueres>

    fun hentKlagevedtakSomSkalDistribueresForSakId(sakId: SakId): List<VedtakSomSkalDistribueres>
}
