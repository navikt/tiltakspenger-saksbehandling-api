package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.common.JournalpostIdGenerator
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.vedtak.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.JournalførVedtaksbrevGateway

class JournalførFakeVedtaksbrevGateway(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførVedtaksbrevGateway {

    private val data = Atomic(mutableMapOf<VedtakId, JournalpostId>())

    override suspend fun journalførVedtaksbrev(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        return data.get()[vedtak.id] ?: journalpostIdGenerator.neste().also {
            data.get().putIfAbsent(vedtak.id, it)
        }
    }
}
