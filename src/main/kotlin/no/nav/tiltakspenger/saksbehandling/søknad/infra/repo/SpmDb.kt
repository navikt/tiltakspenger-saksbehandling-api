package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import java.time.LocalDate

/**
 * Ren mapping mellom søknadens spørsmålstyper og verdiene vi lagrer i `søknad`-tabellen.
 * Rører ikke postgres.
 * Selve radlesingen ligger i `SpmFunctions.kt`.
 */

const val JA = "JA"
const val NEI = "NEI"
const val IKKE_BESVART = "IKKE_BESVART"

const val JA_SUFFIX = "_ja"
const val FOM_SUFFIX = "_fom"
const val PERIODE_SUFFIX = "_periode"
const val TYPE_SUFFIX = "_type"

fun tilPeriodeSpm(
    type: String?,
    fraOgMed: LocalDate?,
    tilOgMed: LocalDate?,
): Søknad.PeriodeSpm =
    when (type) {
        JA -> Søknad.PeriodeSpm.Ja(fraOgMed, tilOgMed)
        NEI -> Søknad.PeriodeSpm.Nei
        IKKE_BESVART -> Søknad.PeriodeSpm.IkkeBesvart
        else -> throw IllegalArgumentException("Ugyldig type $type")
    }

fun tilFraOgMedDatoSpm(
    type: String?,
    fraOgMed: LocalDate?,
): Søknad.FraOgMedDatoSpm =
    when (type) {
        JA -> Søknad.FraOgMedDatoSpm.Ja(fraOgMed)
        NEI -> Søknad.FraOgMedDatoSpm.Nei
        IKKE_BESVART -> Søknad.FraOgMedDatoSpm.IkkeBesvart
        else -> throw IllegalArgumentException("Ugyldig type $type")
    }

fun tilJaNeiSpm(type: String?): Søknad.JaNeiSpm =
    when (type) {
        JA -> Søknad.JaNeiSpm.Ja
        NEI -> Søknad.JaNeiSpm.Nei
        IKKE_BESVART -> Søknad.JaNeiSpm.IkkeBesvart
        else -> throw IllegalArgumentException("Ugyldig type $type")
    }

fun Map<String, Søknad.PeriodeSpm>.toPeriodeSpmParams(): Map<String, Any?> =
    this
        .flatMap { (k, v) ->
            listOf(
                k + TYPE_SUFFIX to lagrePeriodeSpmType(v),
                k + JA_SUFFIX to lagrePeriodeSpmJa(v),
                k + PERIODE_SUFFIX to lagrePeriodeSpmPeriode(v),
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

fun lagrePeriodeSpmPeriode(periodeSpm: Søknad.PeriodeSpm): String? =
    when (periodeSpm) {
        is Søknad.PeriodeSpm.Ja -> tilDbPeriode(periodeSpm.fraOgMed, periodeSpm.tilOgMed)
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
