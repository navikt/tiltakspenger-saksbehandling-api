package no.nav.tiltakspenger.saksbehandling.common

import java.time.LocalDateTime
import java.time.Month
import java.time.temporal.ChronoUnit

fun Int.januarDateTime(year: Int): LocalDateTime =
    LocalDateTime
        .of(
            year,
            Month.JANUARY,
            this,
            12,
            0,
        ).truncatedTo(ChronoUnit.MILLIS)
