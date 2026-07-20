package no.nav.tiltakspenger.saksbehandling.behandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførteDokumenter
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

interface JournalførRammevedtaksbrevKlient {
    suspend fun journalførVedtaksbrevForRammevedtak(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter>
}
