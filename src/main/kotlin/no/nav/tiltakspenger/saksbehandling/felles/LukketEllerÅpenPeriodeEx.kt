package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.periode.LukketEllerÅpenPeriode
import no.nav.tiltakspenger.libs.periode.Overlapp
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import java.time.LocalDate

/**
 * En [Periode] når begge datoene er kjent, ellers en [ÅpenPeriode].
 * Samme konvensjon som tiltaksdeltakelsene fra libs, slik at de samme datoene bare kan uttrykkes på én måte.
 */
fun lukketEllerÅpenPeriode(fraOgMed: LocalDate?, tilOgMed: LocalDate?): LukketEllerÅpenPeriode =
    if (fraOgMed != null && tilOgMed != null) Periode(fraOgMed, tilOgMed) else ÅpenPeriode(fraOgMed, tilOgMed)

/**
 * Kravet bak [lukketEllerÅpenPeriode]: med begge datoene på plass skal perioden være en [Periode].
 */
fun LukketEllerÅpenPeriode.krevLukketNårBeggeDatoeneErKjent() {
    require(this !is ÅpenPeriode || this.periode == null) {
        "Med begge datoene på plass skal perioden være en Periode, ikke en ÅpenPeriode"
    }
}

/** Den lukkede perioden, eller `null` dersom minst én av datoene mangler. */
val LukketEllerÅpenPeriode.lukketPeriode: Periode? get() = this as? Periode

/**
 * Se [ÅpenPeriode.overlapperMed].
 */
fun LukketEllerÅpenPeriode.overlapperMed(periode: Periode): Overlapp = tilÅpenPeriode().overlapperMed(periode)

/**
 * Se [ÅpenPeriode.overlapperMed].
 */
fun LukketEllerÅpenPeriode.overlapperMed(other: LukketEllerÅpenPeriode): Overlapp =
    tilÅpenPeriode().overlapperMed(other.tilÅpenPeriode())

private fun LukketEllerÅpenPeriode.tilÅpenPeriode(): ÅpenPeriode = when (this) {
    is Periode -> ÅpenPeriode(fraOgMed, tilOgMed)
    is ÅpenPeriode -> this
}
