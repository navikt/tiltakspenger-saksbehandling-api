package no.nav.tiltakspenger.saksbehandling.journalnotat

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførteDokumenter

interface JournalførJournalnotatKlient {
    suspend fun journalførJournalnotat(
        journalnotat: Journalnotat,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter>
}
