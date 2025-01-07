package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.common.JournalpostIdGenerator
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.ports.JournalførMeldekortGateway

class JournalførFakeMeldekortGateway(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførMeldekortGateway {

    private val data = Atomic(mutableMapOf<MeldekortId, JournalpostId>())

    override suspend fun journalførMeldekort(
        meldekort: MeldekortBehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        return data.get()[meldekort.id] ?: journalpostIdGenerator.neste().also {
            data.get().putIfAbsent(meldekort.id, it)
        }
    }
}
