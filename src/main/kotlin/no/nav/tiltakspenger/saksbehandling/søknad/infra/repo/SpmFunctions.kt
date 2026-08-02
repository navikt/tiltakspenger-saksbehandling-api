package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad

/**
 * Leser søknadens spørsmålskolonner ut av raden.
 * Selve oversettelsen mellom lagret verdi og domenetype ligger i `SpmDb.kt`.
 */

fun Row.periodeSpm(navn: String): Søknad.PeriodeSpm =
    tilPeriodeSpm(
        type = stringOrNull(navn + TYPE_SUFFIX),
        fraOgMed = localDateOrNull(navn + FOM_SUFFIX),
        tilOgMed = localDateOrNull(navn + TOM_SUFFIX),
    )

fun Row.fraOgMedDatoSpm(navn: String): Søknad.FraOgMedDatoSpm =
    tilFraOgMedDatoSpm(
        type = stringOrNull(navn + TYPE_SUFFIX),
        fraOgMed = localDateOrNull(navn + FOM_SUFFIX),
    )

fun Row.jaNeiSpm(navn: String): Søknad.JaNeiSpm = tilJaNeiSpm(stringOrNull(navn + TYPE_SUFFIX))
