package no.nav.tiltakspenger.saksbehandling.felles

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

fun NonEmptyList<Periode>.tilUnikePerioderUtenOverlapp(): NonEmptyList<Periode> {
    val unikePerioder: NonEmptyList<Periode> = this.distinct()

    if (unikePerioder.size == 1) {
        return unikePerioder
    }

    val sisteTilOgMed: LocalDate = unikePerioder.map { it.tilOgMed }.maxOf { it }

    val fraOgMedDatoer = unikePerioder.toList().flatMap { periode ->
        if (periode.tilOgMed == sisteTilOgMed) {
            listOf(periode.fraOgMed)
        } else {
            listOf(periode.fraOgMed, periode.tilOgMed.plusDays(1))
        }
    }.distinct().sorted()

    return fraOgMedDatoer.mapIndexed { index, fraOgMed ->
        val nesteFraOgMed = fraOgMedDatoer.getOrNull(index + 1)

        val tilOgMed = if (nesteFraOgMed != null) nesteFraOgMed.minusDays(1) else sisteTilOgMed

        Periode(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
        )
    }.toNonEmptyListOrNull()!!
}
