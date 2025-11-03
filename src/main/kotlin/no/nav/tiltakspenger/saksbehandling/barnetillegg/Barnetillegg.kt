package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.inneholderOverlapp
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val periodisering: SammenhengendePeriodisering<AntallBarn>,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
) {
    val harBarnetillegg: Boolean by lazy {
        periodisering.any { it.verdi != AntallBarn.ZERO }
    }

    fun krympPeriode(periode: Periode): Barnetillegg {
        val krympetPeriodisering = periodisering.krymp(periode) as SammenhengendePeriodisering<AntallBarn>
        return this.copy(periodisering = krympetPeriodisering)
    }

    companion object {

        fun periodiserOgFyllUtHullMed0(
            perioder: BarnetilleggPerioder,
            begrunnelse: BegrunnelseVilkårsvurdering?,
            virkningsperiode: Periode,
        ) = Barnetillegg(
            periodisering = perioder.periodiserOgFyllUtHullMed0(virkningsperiode),
            begrunnelse = begrunnelse,
        )

        fun utenBarnetillegg(virkningsperiode: Periode) = Barnetillegg(
            periodisering = SammenhengendePeriodisering(
                PeriodeMedVerdi(
                    periode = virkningsperiode,
                    verdi = AntallBarn.ZERO,
                ),
            ),
            begrunnelse = null,
        )
    }
}

private typealias BarnetilleggPerioder = List<Pair<Periode, AntallBarn>>

/**
 * Periodiserer og fyller ut hull med 0.
 * @throws IllegalArgumentException Dersom periodene er utenfor virkningsperioden eller overlapper.
 */
private fun BarnetilleggPerioder.periodiserOgFyllUtHullMed0(virkningsperiode: Periode): SammenhengendePeriodisering<AntallBarn> {
    if (this.map { it.first }.inneholderOverlapp()) {
        throw IllegalArgumentException("Periodene kan ikke overlappe")
    }
    return this.tilPeriodisering().utvid(AntallBarn.ZERO, virkningsperiode) as SammenhengendePeriodisering
}
