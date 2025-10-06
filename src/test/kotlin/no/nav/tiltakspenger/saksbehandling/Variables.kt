package no.nav.tiltakspenger.saksbehandling

import no.nav.tiltakspenger.libs.dato.januar
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

private val startPoint = 1.januar(2025)

// TODO - flytt alt nedenfor til libs

/** Fixed UTC Clock at 2025-01-01T01:02:03.456789000Z */
val fixedClock: Clock = fixedClockAt(startPoint)

fun fixedClockAt(date: LocalDate = startPoint): Clock =
    Clock.fixed(date.atTime(1, 2, 3, 456789000).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

/** Fixed UTC clock at 2025-02-08T01:02:03.456789000Z */
val enUkeEtterFixedClock: Clock = fixedClock.plus(7, ChronoUnit.DAYS)

/** Fixed UTC Clock */
fun Clock.plus(amountToAdd: Long, unit: TemporalUnit): Clock =
    Clock.fixed(this.instant().plus(amountToAdd, unit), ZoneOffset.UTC)
