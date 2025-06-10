@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

class JournalførFakeRammevedtaksbrevKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførRammevedtaksbrevKlient {

    private val data = Atomic(mutableMapOf<VedtakId, JournalpostId>())

    override suspend fun journalførVedtaksbrevForRammevedtak(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        return data.get()[vedtak.id] ?: journalpostIdGenerator.neste().also {
            data.get().putIfAbsent(vedtak.id, it)
        }
    }
}
