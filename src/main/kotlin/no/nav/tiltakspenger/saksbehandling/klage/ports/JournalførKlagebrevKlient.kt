package no.nav.tiltakspenger.saksbehandling.klage.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførteDokumenter
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak

interface JournalførKlagebrevKlient {
    suspend fun journalførAvvisningsvedtakForKlagevedtak(
        klagevedtak: Klagevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter>

    suspend fun journalførInnstillingsbrevForOpprettholdtKlagebehandling(
        klagebehandling: Klagebehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter>
}
