package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

class SafJournalpostFakeClient : SafJournalpostClient {
    override suspend fun hentJournalpost(journalpostId: JournalpostId): Journalpost? {
        return null
    }
}
