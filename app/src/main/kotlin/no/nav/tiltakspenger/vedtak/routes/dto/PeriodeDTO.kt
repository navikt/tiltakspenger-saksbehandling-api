package no.nav.tiltakspenger.vedtak.routes.dto

import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

/**
 * Skal kun brukes i route-laget. Dersom du trenger den til andre ser/des, bør den flyttes til common-lib.
 */
data class PeriodeDTO(
    val fraOgMed: String,
    val tilOgMed: String,
) {
    fun toDomain(): Periode = Periode(LocalDate.parse(fraOgMed), LocalDate.parse(tilOgMed))
}

fun Periode.toDTO(): PeriodeDTO = PeriodeDTO(fraOgMed.toString(), tilOgMed.toString())
