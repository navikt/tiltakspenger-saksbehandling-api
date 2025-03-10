package no.nav.tiltakspenger.vedtak.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.vedtak.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak.Rammevedtak

interface JournalførVedtaksbrevGateway {
    suspend fun journalførVedtaksbrev(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId
}
