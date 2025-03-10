package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.common.JournalpostIdGenerator
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.vedtak.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.vedtak.meldekort.ports.JournalførMeldekortGateway

class JournalførFakeMeldekortGateway(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførMeldekortGateway {

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
