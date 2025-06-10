@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient

class JournalførFakeMeldekortKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførMeldekortKlient {

    private val data = Atomic(mutableMapOf<MeldekortId, JournalpostId>())

    override suspend fun journalførMeldekortBehandling(
        meldekortBehandling: MeldekortBehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        return data.get()[meldekortBehandling.id] ?: journalpostIdGenerator.neste().also {
            data.get().putIfAbsent(meldekortBehandling.id, it)
        }
    }
}
