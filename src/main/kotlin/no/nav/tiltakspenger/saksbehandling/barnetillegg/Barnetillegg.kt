package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barnetillegget i en innvilgelse.
 * Uansett versjon gir [periodisering] antall barn med barnetillegg per dag i innvilgelsesperiodene, og det er den beregning, brev og statistikk bygger på.
 *
 * - [BarnetilleggV1]: antall barn per periode, uten å vite hvilke barn, og kun for eldre data.
 * - [BarnetilleggV2]: de valgte barna, som antall barn utledes fra sammen med innvilgelsesperiodene.
 */
sealed interface Barnetillegg {
    val begrunnelse: Begrunnelse?

    /**
     * Antall barn med barnetillegg per dag i [innvilgelsesperioder], som må være behandlingens innvilgelsesperioder.
     * [BarnetilleggV1] har lagret antallet per periode, og gir det uendret.
     */
    fun periodisering(innvilgelsesperioder: NonEmptyList<Periode>): IkkeTomPeriodisering<AntallBarn>

    fun harBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Boolean {
        return periodisering(innvilgelsesperioder).any { it.verdi != AntallBarn.ZERO }
    }

    /**
     * Tilpasser barnetillegget til [perioder], som er nye innvilgelsesperioder innenfor de tidligere.
     * Gir null dersom ingen av dagene i barnetillegget er innenfor [perioder].
     */
    fun krympTilPerioder(perioder: List<Periode>): Barnetillegg?

    companion object {
        fun utenBarnetillegg(): BarnetilleggV2 = BarnetilleggV2.utenBarnetillegg()
    }
}
