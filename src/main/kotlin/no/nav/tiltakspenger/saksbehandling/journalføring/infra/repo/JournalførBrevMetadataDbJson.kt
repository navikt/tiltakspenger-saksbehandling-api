package no.nav.tiltakspenger.saksbehandling.journalføring.infra.repo

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import java.time.LocalDateTime

private data class JournalførBrevMetadataDbJson(
    val requestBody: String,
    val responseStatus: String,
    val responseBody: String,
    val journalføringsTidspunkt: String,
)

fun JournalførBrevMetadata.toDbJson(): String {
    val dbJson = JournalførBrevMetadataDbJson(
        requestBody = this.requestBody,
        responseStatus = this.responseStatus,
        responseBody = this.responseBody,
        journalføringsTidspunkt = this.journalføringsTidspunkt.toString(),
    )
    return serialize(dbJson)
}
