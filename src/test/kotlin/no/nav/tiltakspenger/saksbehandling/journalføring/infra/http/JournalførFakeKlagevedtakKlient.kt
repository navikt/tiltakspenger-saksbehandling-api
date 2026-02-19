@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.ports.JournalførKlagebrevKlient

class JournalførFakeKlagevedtakKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførKlagebrevKlient {

    private val data = Atomic(mutableMapOf<Ulid, JournalpostId>())

    val journalførBrevMetadata by lazy {
        JournalførBrevMetadata(
            requestBody = "requestBody",
            responseStatus = "responseStatus",
            responseBody = "responseBody",
            journalføringsTidspunkt = nå(fixedClock),
        )
    }

    override suspend fun journalførAvvisningsvedtakForKlagevedtak(
        klagevedtak: Klagevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        return (
            data.get()[klagevedtak.id] ?: journalpostIdGenerator.neste().also {
                data.get().putIfAbsent(klagevedtak.id, it)
            }
            ) to journalførBrevMetadata
    }

    override suspend fun journalførInnstillingsbrevForOpprettholdtKlagebehandling(
        klagebehandling: Klagebehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        return (
            data.get()[klagebehandling.id] ?: journalpostIdGenerator.neste().also {
                data.get().putIfAbsent(klagebehandling.id, it)
            }
            ) to journalførBrevMetadata
    }
}
