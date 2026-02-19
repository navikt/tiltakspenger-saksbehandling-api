package no.nav.tiltakspenger.saksbehandling.journalføring

import java.time.LocalDateTime

class JournalførBrevMetadata(
    val requestBody: String,
    val responseStatus: String,
    val responseBody: String,
    val journalføringsTidspunkt: LocalDateTime,
)
