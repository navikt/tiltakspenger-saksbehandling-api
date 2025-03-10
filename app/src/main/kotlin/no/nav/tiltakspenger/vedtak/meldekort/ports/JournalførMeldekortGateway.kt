package no.nav.tiltakspenger.vedtak.meldekort.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.vedtak.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandling

interface JournalførMeldekortGateway {
    suspend fun journalførMeldekortBehandling(
        meldekortBehandling: MeldekortBehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId
}
