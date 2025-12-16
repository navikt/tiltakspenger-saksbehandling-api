package no.nav.tiltakspenger.saksbehandling.felles

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay

val fasteHelligdager = listOf<MonthDay>(
    MonthDay.of(Month.JANUARY, 1),
    MonthDay.of(Month.MAY, 1),
    MonthDay.of(Month.MAY, 17),
    MonthDay.of(Month.DECEMBER, 25),
    MonthDay.of(Month.DECEMBER, 26),
    MonthDay.of(Month.DECEMBER, 31),
)

fun LocalDate.erHelg(): Boolean {
    return this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY
}

fun LocalDate.erHverdag(): Boolean {
    return !this.erHelg()
}

fun LocalDate.erFastHelligdag(): Boolean {
    val monthDay = MonthDay.of(this.month, this.dayOfMonth)
    return monthDay in fasteHelligdager
}

fun max(d1: LocalDate, d2: LocalDate): LocalDate {
    return if (d1.isAfter(d2)) d1 else d2
}

fun min(d1: LocalDate, d2: LocalDate): LocalDate {
    return if (d1.isBefore(d2)) d1 else d2
}
