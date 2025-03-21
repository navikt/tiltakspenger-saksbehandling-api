package no.nav.tiltakspenger.saksbehandling

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

// TODO - erstatt denne med int.måned() (vi må flytte ex. fns til libs)
private val startPoint = LocalDate.parse("2025-01-01")

// TODO - flytt alt nedenfor til libs
/** Fixed UTC Clock at 2025-01-01T01:02:03.456789000Z */
val fixedClock: Clock = Clock.fixed(startPoint.atTime(1, 2, 3, 456789000).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

fun fixedClockAt(date: LocalDate = startPoint): Clock =
    Clock.fixed(date.atTime(1, 2, 3, 456789000).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

/** Fixed UTC clock at 2025-02-08T01:02:03.456789000Z */
val enUkeEtterFixedClock: Clock = fixedClock.plus(7, ChronoUnit.DAYS)

/** Fixed UTC Clock */
fun Clock.plus(amountToAdd: Long, unit: TemporalUnit): Clock =
    Clock.fixed(this.instant().plus(amountToAdd, unit), ZoneOffset.UTC)
