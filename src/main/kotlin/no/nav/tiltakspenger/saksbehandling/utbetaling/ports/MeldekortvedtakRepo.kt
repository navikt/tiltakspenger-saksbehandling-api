package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import java.time.LocalDateTime

interface MeldekortvedtakRepo {
    fun lagre(vedtak: Meldekortvedtak, context: TransactionContext? = null)

    fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    )

    fun hentDeSomSkalJournalføres(limit: Int = 10): List<Meldekortvedtak>
}
