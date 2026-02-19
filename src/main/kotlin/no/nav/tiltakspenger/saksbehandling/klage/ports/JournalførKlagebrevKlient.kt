package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak

interface JournalførKlagebrevKlient {
    suspend fun journalførAvvisningsvedtakForKlagevedtak(
        klagevedtak: Klagevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata>

    suspend fun journalførInnstillingsbrevForOpprettholdtKlagebehandling(
        klagebehandling: Klagebehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata>
}
