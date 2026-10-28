package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.libs.periode.LukketEllerÅpenPeriode

/**
 * En periode der [fraOgMed] og/eller [tilOgMed] kan mangle.
 * Brukes for både [no.nav.tiltakspenger.libs.periode.Periode] og [no.nav.tiltakspenger.libs.periode.ÅpenPeriode], slik at frontend får samme form uansett.
 */
data class ÅpenPeriodeDTO(
    val fraOgMed: String?,
    val tilOgMed: String?,
)

fun LukketEllerÅpenPeriode.toÅpenPeriodeDTO(): ÅpenPeriodeDTO = ÅpenPeriodeDTO(
    fraOgMed = fraOgMed?.toString(),
    tilOgMed = tilOgMed?.toString(),
)
