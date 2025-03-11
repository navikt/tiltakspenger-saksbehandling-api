package no.nav.tiltakspenger.saksbehandling.repository.felles

import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

/**
 * Skal kun brukes i route-laget. Dersom du trenger den til andre ser/des, bør den flyttes til common-lib.
 */
data class PeriodeDbJson(
    val fraOgMed: String,
    val tilOgMed: String,
) {
    fun toDomain(): Periode = Periode(LocalDate.parse(fraOgMed), LocalDate.parse(tilOgMed))
}

fun Periode.toDbJson(): PeriodeDbJson = PeriodeDbJson(fraOgMed.toString(), tilOgMed.toString())
