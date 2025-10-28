package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

interface SafJournalpostClient {
    suspend fun hentJournalpost(journalpostId: JournalpostId): Journalpost?
}
