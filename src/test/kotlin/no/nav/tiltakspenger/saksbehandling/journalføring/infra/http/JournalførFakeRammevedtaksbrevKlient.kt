@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

class JournalførFakeRammevedtaksbrevKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførRammevedtaksbrevKlient {

    private val data = Atomic(mutableMapOf<VedtakId, JournalpostId>())
    val journalførBrevMetadata by lazy {
        JournalførBrevMetadata(
            requestBody = "requestBody",
            responseStatus = "responseStatus",
            responseBody = "responseBody",
            journalføringsTidspunkt = nå(fixedClock),
        )
    }

    override suspend fun journalførVedtaksbrevForRammevedtak(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        return (
            data.get()[vedtak.id] ?: journalpostIdGenerator.neste().also {
                data.get().putIfAbsent(vedtak.id, it)
            }
            ) to journalførBrevMetadata
    }
}
