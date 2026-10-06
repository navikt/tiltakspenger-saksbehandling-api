package no.nav.tiltakspenger.saksbehandling.journalnotat

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.LocalDateTime

/**
 * Kvittering for at det interne notatet om et vedtak er journalført i Joark.
 * Lagres på vedtaket, og brukes også for å unngå at notatet journalføres flere ganger.
 */
data class Journalføringsnotat(
    val journalpostId: JournalpostId,
    val journalføringstidspunkt: LocalDateTime,
)
