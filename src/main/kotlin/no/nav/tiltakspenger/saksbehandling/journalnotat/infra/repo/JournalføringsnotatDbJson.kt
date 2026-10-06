package no.nav.tiltakspenger.saksbehandling.journalnotat.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalføringsnotat
import java.time.LocalDateTime

/** Lagres i `journalføringsnotat`-kolonnen, som finnes både på `rammevedtak` og `meldekortvedtak`. */
private data class JournalføringsnotatDbJson(
    val journalpostId: String,
    val journalføringstidspunkt: String,
)

fun Journalføringsnotat.toDbJson(): String = serialize(
    JournalføringsnotatDbJson(
        journalpostId = journalpostId.toString(),
        journalføringstidspunkt = journalføringstidspunkt.toString(),
    ),
)

fun String.toJournalføringsnotat(): Journalføringsnotat {
    val json = deserialize<JournalføringsnotatDbJson>(this)
    return Journalføringsnotat(
        journalpostId = JournalpostId(json.journalpostId),
        journalføringstidspunkt = LocalDateTime.parse(json.journalføringstidspunkt),
    )
}
