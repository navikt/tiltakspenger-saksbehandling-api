package no.nav.tiltakspenger.saksbehandling.journalpost

import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient

class HentJournalpostDokumentService(
    private val journalpostClient: SafJournalpostClient,
) {
    suspend fun hent(
        command: HentDokumentCommand,
    ): PdfA {
        return journalpostClient.hentDokument(command)
    }
}
