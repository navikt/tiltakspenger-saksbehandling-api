package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

interface JournalførVedtaksbrevGateway {
    suspend fun journalførVedtaksbrev(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId
}
