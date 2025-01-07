package no.nav.tiltakspenger.meldekort.ports

import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling

interface JournalførMeldekortGateway {
    suspend fun journalførMeldekort(
        meldekort: MeldekortBehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId
}
