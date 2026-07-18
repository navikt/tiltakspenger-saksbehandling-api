package no.nav.tiltakspenger.saksbehandling.journalpost

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient

class HentJournalpostDokumentService(
    private val journalpostClient: SafJournalpostClient,
) {
    suspend fun hent(
        command: HentDokumentCommand,
    ): Either<HttpKlientError, PdfA> {
        return journalpostClient.hentDokument(command)
    }
}
