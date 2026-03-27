@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.atomic.Atomic
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.DokumentInfoIdGeneratorGenerator
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.ports.JournalførKlagebrevKlient

class JournalførFakeKlagevedtakKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
    private val dokumentInfoIdGeneratorGenerator: DokumentInfoIdGeneratorGenerator,
) : JournalførKlagebrevKlient {

    private val journalpostIdData = Atomic(mutableMapOf<Ulid, JournalpostId>())
    private val dokumentInfoIdData = Atomic(mutableMapOf<Ulid, List<DokumentInfoId>>())

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
    ): JournalførteDokumenter {
        return JournalførteDokumenter(
            journalpostId = journalpostIdData.get()[klagevedtak.id] ?: journalpostIdGenerator.generer().also {
                journalpostIdData.get().putIfAbsent(klagevedtak.id, it)
            },
            dokumentInfoIder = (
                dokumentInfoIdData.get()[klagevedtak.id]
                    ?: listOf(dokumentInfoIdGeneratorGenerator.generer())
                ).toNonEmptyListOrThrow()
                .also {
                    dokumentInfoIdData.get().putIfAbsent(klagevedtak.id, it.toList())
                },
            metadata = journalførBrevMetadata,
        )
    }

    override suspend fun journalførInnstillingsbrevForOpprettholdtKlagebehandling(
        klagebehandling: Klagebehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalførteDokumenter {
        return JournalførteDokumenter(
            journalpostId = journalpostIdData.get()[klagebehandling.id] ?: journalpostIdGenerator.generer().also {
                journalpostIdData.get().putIfAbsent(klagebehandling.id, it)
            },
            dokumentInfoIder = (
                dokumentInfoIdData.get()[klagebehandling.id]
                    ?: listOf(dokumentInfoIdGeneratorGenerator.generer())
                ).toNonEmptyListOrThrow()
                .also {
                    dokumentInfoIdData.get().putIfAbsent(klagebehandling.id, it.toList())
                },
            metadata = journalførBrevMetadata,
        )
    }
}
