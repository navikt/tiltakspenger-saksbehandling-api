package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import kotliquery.Row
import no.nav.tiltakspenger.libs.periode.Periode
import org.postgresql.util.PGobject
import java.time.LocalDate

/**
 * Skal kun brukes i db-laget. Dersom du trenger den til andre ser/des, bør den flyttes til common-lib.
 */
data class PeriodeDbJson(
    val fraOgMed: String,
    val tilOgMed: String,
) {
    fun toDomain(): Periode = Periode(LocalDate.parse(fraOgMed), LocalDate.parse(tilOgMed))
}

fun Periode.toDbJson(): PeriodeDbJson = PeriodeDbJson(fraOgMed.toString(), tilOgMed.toString())

fun Periode.tilDbPeriode(): String {
    return "(${this.fraOgMed},${this.tilOgMed})"
}

fun Row.periode(column: String): Periode {
    return periodeOrNull(column)!!
}

fun Row.periodeOrNull(column: String): Periode? {
    val pgObject = underlying.getObject(column, PGobject::class.java)
    val value = pgObject?.value ?: return null
    return parsePeriode(value)
}

private fun parsePeriode(value: String): Periode {
    val (fraOgMed, tilOgMed) = value
        .removeSurrounding("(", ")")
        .split(",")
    return Periode(
        LocalDate.parse(fraOgMed),
        LocalDate.parse(tilOgMed),
    )
}
