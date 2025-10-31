package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad

private const val JA = "JA"
private const val NEI = "NEI"
private const val IKKE_BESVART = "IKKE_BESVART"

private const val JA_SUFFIX = "_ja"
private const val FOM_SUFFIX = "_fom"
private const val TOM_SUFFIX = "_tom"
private const val TYPE_SUFFIX = "_type"

fun Row.periodeSpm(navn: String): Søknad.PeriodeSpm {
    val type = stringOrNull(navn + TYPE_SUFFIX)
    val fom = localDateOrNull(navn + FOM_SUFFIX)
    val tom = localDateOrNull(navn + TOM_SUFFIX)
    return when (type) {
        JA -> {
            checkNotNull(fom) { "Fom må være satt om periodespørsmål er JA" }
            checkNotNull(tom) { "Tom må være satt om periodespørsmål er JA" }
            Søknad.PeriodeSpm.Ja(Periode(fom, tom))
        }
        NEI -> Søknad.PeriodeSpm.Nei
        IKKE_BESVART -> Søknad.PeriodeSpm.IkkeBesvart
        else -> throw IllegalArgumentException("Ugyldig type")
    }
}

fun Row.fraOgMedDatoSpm(navn: String): Søknad.FraOgMedDatoSpm {
    val type = stringOrNull(navn + TYPE_SUFFIX)
    val fom = localDateOrNull(navn + FOM_SUFFIX)
    return when (type) {
        JA -> {
            checkNotNull(fom) { "Fom må være satt i fraOgMedDatoSpm om svaret er JA" }
            Søknad.FraOgMedDatoSpm.Ja(fom)
        }
        NEI -> Søknad.FraOgMedDatoSpm.Nei
        IKKE_BESVART -> Søknad.FraOgMedDatoSpm.IkkeBesvart
        else -> throw IllegalArgumentException("Ugyldig type")
    }
}

fun Row.jaNeiSpm(navn: String): Søknad.JaNeiSpm =
    when (stringOrNull(navn + TYPE_SUFFIX)) {
        JA -> Søknad.JaNeiSpm.Ja
        NEI -> Søknad.JaNeiSpm.Nei
        IKKE_BESVART -> Søknad.JaNeiSpm.IkkeBesvart
        else -> throw IllegalArgumentException("Ugyldig type")
    }

fun Map<String, Søknad.PeriodeSpm>.toPeriodeSpmParams(): Map<String, Any?> =
    this
        .flatMap { (k, v) ->
            listOf(
                k + TYPE_SUFFIX to lagrePeriodeSpmType(v),
                k + JA_SUFFIX to lagrePeriodeSpmJa(v),
                k + FOM_SUFFIX to lagrePeriodeSpmFra(v),
                k + TOM_SUFFIX to lagrePeriodeSpmTil(v),
            )
        }.associate {
            it.first to it.second as Any?
        }

fun Map<String, Søknad.FraOgMedDatoSpm>.toFraOgMedDatoSpmParams(): Map<String, Any?> =
    this
        .flatMap { (k, v) ->
            listOf(
                k + TYPE_SUFFIX to lagreFraOgMedDatoSpmType(v),
                k + JA_SUFFIX to lagreFraOgMedDatoSpmJa(v),
                k + FOM_SUFFIX to lagreFraOgMedDatoSpmFra(v),
            )
        }.associate {
            it.first to it.second as Any?
        }

fun Map<String, Søknad.JaNeiSpm>.toJaNeiSpmParams(): Map<String, Any?> =
    this
        .flatMap { (k, v) ->
            listOf(
                k + TYPE_SUFFIX to lagreJaNeiSpmType(v),
            )
        }.associate {
            it.first to it.second as Any?
        }

fun lagrePeriodeSpmType(periodeSpm: Søknad.PeriodeSpm) =
    when (periodeSpm) {
        is Søknad.PeriodeSpm.Ja -> JA
        is Søknad.PeriodeSpm.Nei -> NEI
        is Søknad.PeriodeSpm.IkkeBesvart -> IKKE_BESVART
    }

fun lagrePeriodeSpmJa(periodeSpm: Søknad.PeriodeSpm) =
    when (periodeSpm) {
        is Søknad.PeriodeSpm.Ja -> true
        is Søknad.PeriodeSpm.Nei, Søknad.PeriodeSpm.IkkeBesvart -> false
    }

fun lagrePeriodeSpmFra(periodeSpm: Søknad.PeriodeSpm) =
    when (periodeSpm) {
        is Søknad.PeriodeSpm.Ja -> periodeSpm.periode.fraOgMed
        is Søknad.PeriodeSpm.Nei, Søknad.PeriodeSpm.IkkeBesvart -> null
    }

fun lagrePeriodeSpmTil(periodeSpm: Søknad.PeriodeSpm) =
    when (periodeSpm) {
        is Søknad.PeriodeSpm.Ja -> periodeSpm.periode.tilOgMed
        is Søknad.PeriodeSpm.Nei, Søknad.PeriodeSpm.IkkeBesvart -> null
    }

fun lagreFraOgMedDatoSpmType(fraOgMedDatoSpm: Søknad.FraOgMedDatoSpm) =
    when (fraOgMedDatoSpm) {
        is Søknad.FraOgMedDatoSpm.Ja -> JA
        is Søknad.FraOgMedDatoSpm.Nei -> NEI
        is Søknad.FraOgMedDatoSpm.IkkeBesvart -> IKKE_BESVART
    }

fun lagreFraOgMedDatoSpmJa(fraOgMedDatoSpm: Søknad.FraOgMedDatoSpm) =
    when (fraOgMedDatoSpm) {
        is Søknad.FraOgMedDatoSpm.Ja -> true
        is Søknad.FraOgMedDatoSpm.Nei, Søknad.FraOgMedDatoSpm.IkkeBesvart -> false
    }

fun lagreFraOgMedDatoSpmFra(fraOgMedDatoSpm: Søknad.FraOgMedDatoSpm) =
    when (fraOgMedDatoSpm) {
        is Søknad.FraOgMedDatoSpm.Ja -> fraOgMedDatoSpm.fra
        is Søknad.FraOgMedDatoSpm.Nei, Søknad.FraOgMedDatoSpm.IkkeBesvart -> null
    }

fun lagreJaNeiSpmType(jaNeiSpm: Søknad.JaNeiSpm): String =
    when (jaNeiSpm) {
        is Søknad.JaNeiSpm.Ja -> JA
        is Søknad.JaNeiSpm.Nei -> NEI
        is Søknad.JaNeiSpm.IkkeBesvart -> IKKE_BESVART
    }

fun lagreJaNeiSpmJa(jaNeiSpm: Søknad.JaNeiSpm) =
    when (jaNeiSpm) {
        is Søknad.JaNeiSpm.Ja -> true
        is Søknad.JaNeiSpm.Nei, Søknad.JaNeiSpm.IkkeBesvart -> false
    }
