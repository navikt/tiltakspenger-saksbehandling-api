package no.nav.tiltakspenger.saksbehandling.felles

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Konverterer en ISO 8601-formatert string til en LocalDateTime.
 */
fun String.tilLocalDateTime(): LocalDateTime =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
