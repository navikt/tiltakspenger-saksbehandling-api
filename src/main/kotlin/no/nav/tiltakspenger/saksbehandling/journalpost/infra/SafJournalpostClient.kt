package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.HentDokumentCommand

interface SafJournalpostClient {
    suspend fun hentJournalpost(journalpostId: JournalpostId): Journalpost?

    /**
     * kaster exception ved feil
     */
    suspend fun hentDokument(
        command: HentDokumentCommand,
    ): PdfA
}
