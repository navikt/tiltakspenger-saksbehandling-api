package no.nav.tiltakspenger.saksbehandling.felles

import java.time.LocalDate

fun LocalDate.erHelg(): Boolean {
    return this.dayOfWeek.value in listOf(6, 7)
}

fun max(d1: LocalDate, d2: LocalDate): LocalDate {
    return if (d1.isAfter(d2)) d1 else d2
}

fun min(d1: LocalDate, d2: LocalDate): LocalDate {
    return if (d1.isBefore(d2)) d1 else d2
}
