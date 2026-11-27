package no.nav.tiltakspenger.saksbehandling.journalnotat.infra

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførteDokumenter
import no.nav.tiltakspenger.saksbehandling.journalnotat.JournalførJournalnotatKlient
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat

/** Dedupliserer på vedtakId, slik dokarkiv gjør på eksternReferanseId. */
class JournalførFakeJournalnotatKlient(
    private val journalpostIdGenerator: JournalpostIdGenerator,
) : JournalførJournalnotatKlient {

    private val data = Atomic(mutableMapOf<VedtakId, JournalpostId>())

    override suspend fun journalførJournalnotat(
        journalnotat: Journalnotat,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        return JournalførteDokumenter(
            journalpostId = data.get()[journalnotat.vedtakId] ?: journalpostIdGenerator.generer().also {
                data.get().putIfAbsent(journalnotat.vedtakId, it)
            },
            dokumentInfoIder = null,
            metadata = JournalførBrevMetadata(
                requestBody = "requestBody",
                responseStatus = "responseStatus",
                responseBody = "responseBody",
                journalføringsTidspunkt = nå(fixedClock),
            ),
        ).right()
    }
}
