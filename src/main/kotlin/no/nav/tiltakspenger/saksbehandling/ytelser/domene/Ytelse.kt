package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import no.nav.tiltakspenger.libs.periode.Periode

/**
 * En ytelse fra sokos-utbetaldata.
 * [kildetekst] er ytelsestypen slik sokos-utbetaldata sendte den, så saksbehandlere kan sammenligne med andre flater som viser utbetalinger.
 */
sealed interface Ytelse {
    val kildetekst: String?
    val perioder: List<Periode>

    fun overlapperMed(periode: Periode): Boolean = perioder.any { it.overlapperMed(periode) }

    data class Kjent(
        val ytelsetype: Ytelsetype,
        override val kildetekst: String,
        override val perioder: List<Periode>,
    ) : Ytelse

    /**
     * En ytelse med en ytelsestype vi ikke kjenner.
     * [kildetekst] er null når sokos-utbetaldata ikke sendte noen ytelsestype.
     * Spec-en tillater det, men ingen behandling i prod hadde en slik ytelse før 2026-11-04.
     */
    data class Ukjent(
        override val kildetekst: String?,
        override val perioder: List<Periode>,
    ) : Ytelse
}
