package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling

interface JournalførMeldekortGateway {
    suspend fun journalførMeldekortBehandling(
        meldekortBehandling: MeldekortBehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId
}
