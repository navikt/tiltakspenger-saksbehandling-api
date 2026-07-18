package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførteDokumenter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak

interface JournalførMeldekortKlient {
    suspend fun journalførVedtaksbrevForMeldekortvedtak(
        meldekortvedtak: Meldekortvedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter>
}
