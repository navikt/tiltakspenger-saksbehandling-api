package no.nav.tiltakspenger.saksbehandling.felles

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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

fun LocalDate.erHelligdag(): Boolean = erFastHelligdag() || erBevegeligHelligdag()

/** En hverdag som ikke er helligdag. */
fun LocalDate.erVirkedag(): Boolean = erHverdag() && !erHelligdag()

/**
 * Legger [varighet] til tidspunktet, men teller bare med tid på virkedager.
 * Lørdager, søndager og helligdager hoppes over.
 */
fun LocalDateTime.plusVirkedagstid(varighet: Duration): LocalDateTime {
    var tidspunkt = this
    var gjenstår = varighet
    while (true) {
        val nesteDøgn = tidspunkt.toLocalDate().plusDays(1).atStartOfDay()
        if (tidspunkt.toLocalDate().erVirkedag()) {
            val restenAvDøgnet = Duration.between(tidspunkt, nesteDøgn)
            if (gjenstår <= restenAvDøgnet) {
                return tidspunkt + gjenstår
            }
            gjenstår -= restenAvDøgnet
        }
        tidspunkt = nesteDøgn
    }
}

private fun LocalDate.erFastHelligdag(): Boolean {
    val monthDay = MonthDay.of(this.month, this.dayOfMonth)
    return monthDay in fasteHelligdager
}

/** Skjærtorsdag, langfredag, 2. påskedag, Kristi himmelfartsdag og 2. pinsedag, som alle følger påskedagen. */
private fun LocalDate.erBevegeligHelligdag(): Boolean {
    val påskedag = påskedag(year)
    return this in listOf(
        påskedag.minusDays(3),
        påskedag.minusDays(2),
        påskedag.plusDays(1),
        påskedag.plusDays(39),
        påskedag.plusDays(50),
    )
}

/**
 * Regner ut 1. påskedag i den gregorianske kalenderen med den anonyme gregorianske algoritmen (Meeus/Jones/Butcher).
 * Navnene på mellomregningene følger algoritmen.
 */
private fun påskedag(år: Int): LocalDate {
    val a = år % 19
    val b = år / 100
    val c = år % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val månedOgDag = h + l - 7 * m + 114
    return LocalDate.of(år, månedOgDag / 31, månedOgDag % 31 + 1)
}

fun max(d1: LocalDate, d2: LocalDate): LocalDate {
    return if (d1.isAfter(d2)) d1 else d2
}

fun min(d1: LocalDate, d2: LocalDate): LocalDate {
    return if (d1.isBefore(d2)) d1 else d2
}
