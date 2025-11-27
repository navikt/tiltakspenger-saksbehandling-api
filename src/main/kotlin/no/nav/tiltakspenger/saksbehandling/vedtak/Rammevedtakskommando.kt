package no.nav.tiltakspenger.saksbehandling.vedtak

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

/**
 * Gyldige/lovlige kommandoer en saksbehandler kan utføre på et rammevedtak.
 * Stans: For at man skal kunne stanse, må vi ha et eller flere gjeldende innvilgede rammevedtak.
 * Opphør: For at man skal kunne opphøre, må vi ha et eller flere gjeldende innvilgede rammevedtak.
 */
sealed interface Rammevedtakskommando {

    /**
     * Foreløpig støtter vi kun å omgjøre et rammevedtak i sin helhet.
     */
    data class Omgjør(
        val tvungenOmgjøringsperiode: Periode,
    ) : Rammevedtakskommando

    /**
     * Opphør av et rammevedtak i sin helhet er ikke implementert enda.
     */
    data class Opphør(
        val innvilgelsesperioder: NonEmptyList<Periode>,
    ) : Rammevedtakskommando

    /**
     * For at man skal kunne stanse, må vi ha et eller flere gjeldende innvilgede rammevedtak.
     * Saksbehandler kan kun velge stans fra og med dato. Saken stanses alltid ut hele den gjeldende innvilgede vedtaksperioden.
     */
    data class Stans(
        val tidligsteFraOgMedDato: LocalDate,
        val tvungenStansTilOgMedDato: LocalDate,
    ) : Rammevedtakskommando
}
