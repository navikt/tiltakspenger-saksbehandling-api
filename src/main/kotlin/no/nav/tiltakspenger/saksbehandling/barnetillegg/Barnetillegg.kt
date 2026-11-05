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
 * - [BarnetilleggV2]: de valgte barna, som antall barn utledes fra.
 */
sealed interface Barnetillegg {
    val periodisering: IkkeTomPeriodisering<AntallBarn>
    val begrunnelse: Begrunnelse?

    val harBarnetillegg: Boolean
        get() = periodisering.any { it.verdi != AntallBarn.ZERO }

    fun harBarnetilleggIPeriode(periode: Periode): Boolean {
        return periodisering.overlappendePeriode(periode).any { it.verdi != AntallBarn.ZERO }
    }

    /** Gir null dersom ingen av dagene i barnetillegget er innenfor [perioder]. */
    fun krympTilPerioder(perioder: List<Periode>): Barnetillegg?

    companion object {
        fun utenBarnetillegg(perioder: NonEmptyList<Periode>): BarnetilleggV2 = BarnetilleggV2.utenBarnetillegg(perioder)
    }
}
