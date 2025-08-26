package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import java.time.LocalDateTime

interface MeldekortVedtakRepo {
    fun lagre(vedtak: MeldekortVedtak, context: TransactionContext? = null)

    fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    )

    fun hentDeSomSkalJournalføres(limit: Int = 10): List<MeldekortVedtak>
}
