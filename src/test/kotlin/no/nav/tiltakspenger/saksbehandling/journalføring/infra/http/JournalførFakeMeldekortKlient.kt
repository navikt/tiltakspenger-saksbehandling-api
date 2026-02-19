@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient

class JournalførFakeMeldekortKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførMeldekortKlient {

    private val data = Atomic(mutableMapOf<VedtakId, JournalpostId>())
    val journalførBrevMetadata by lazy {
        JournalførBrevMetadata(
            requestBody = "requestBody",
            responseStatus = "responseStatus",
            responseBody = "responseBody",
            journalføringsTidspunkt = nå(fixedClock),
        )
    }

    override suspend fun journalførVedtaksbrevForMeldekortvedtak(
        meldekortvedtak: Meldekortvedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        return (
            data.get()[meldekortvedtak.id] ?: journalpostIdGenerator.neste().also {
                data.get().putIfAbsent(meldekortvedtak.id, it)
            }
            ) to journalførBrevMetadata
    }
}
