package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak

interface JournalførMeldekortKlient {
    suspend fun journalførVedtaksbrevForMeldekortvedtak(
        meldekortvedtak: Meldekortvedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata>
}
