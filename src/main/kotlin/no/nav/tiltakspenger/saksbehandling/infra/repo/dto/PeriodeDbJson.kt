package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import kotliquery.Row
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/**
 * Skal kun brukes i db-laget.
 * Dersom du trenger den til andre ser/des, bør den flyttes til common-lib.
 */
data class PeriodeDbJson(
    val fraOgMed: String,
    val tilOgMed: String,
) {
    fun toDomain(): Periode = Periode(LocalDate.parse(fraOgMed), LocalDate.parse(tilOgMed))
}

fun Periode.toDbJson(): PeriodeDbJson = PeriodeDbJson(fraOgMed.toString(), tilOgMed.toString())

/**
 *  Serialize til sql type periode_datoer
 * */
fun Periode.tilDbPeriode(): String {
    return "(${this.fraOgMed},${this.tilOgMed})"
}

fun Row.periode(column: String): Periode {
    return periodeOrNull(column)!!
}

fun Row.periodeOrNull(column: String): Periode? = stringOrNull(column)?.let { parsePeriode(it) }

private fun parsePeriode(value: String): Periode {
    val (fraOgMed, tilOgMed) = value
        .removeSurrounding("(", ")")
        .split(",")
    return Periode(
        LocalDate.parse(fraOgMed),
        LocalDate.parse(tilOgMed),
    )
}

data class ÅpenPeriodeDb(
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
)

/**
 * Serialiserer to nullable datoer til composit-literalen som både `periode` og `periode_open` parses fra.
 * To null-er blir SQL NULL, siden en tom composit `(, )` ikke kan skilles fra NULL ved lesing.
 * Er bare den ene enden satt, avgjør kolonnens domene om verdien er lov: `periode_open` tillater det, `periode` avviser det.
 */
fun tilDbPeriode(fraOgMed: LocalDate?, tilOgMed: LocalDate?): String? =
    if (fraOgMed == null && tilOgMed == null) {
        null
    } else {
        "(${fraOgMed ?: ""},${tilOgMed ?: ""})"
    }

fun Row.åpenPeriodeOrNull(column: String): ÅpenPeriodeDb? = stringOrNull(column)?.let { parseÅpenPeriode(it) }

private fun parseÅpenPeriode(value: String): ÅpenPeriodeDb {
    val (fraOgMed, tilOgMed) = value
        .removeSurrounding("(", ")")
        .split(",")
    return ÅpenPeriodeDb(
        fraOgMed = fraOgMed.ifEmpty { null }?.let { LocalDate.parse(it) },
        tilOgMed = tilOgMed.ifEmpty { null }?.let { LocalDate.parse(it) },
    )
}
