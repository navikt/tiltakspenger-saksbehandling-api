package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.inneholderOverlapp
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import kotlin.collections.flatMap

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val periodisering: Periodisering<AntallBarn>,
    val begrunnelse: Begrunnelse?,
) {
    val harBarnetillegg: Boolean by lazy {
        periodisering.any { it.verdi != AntallBarn.ZERO }
    }

    fun krympPerioder(perioder: List<Periode>): Barnetillegg {
        val krympetPeriodisering = periodisering.perioderMedVerdi.toList().flatMap { bt ->
            bt.periode.overlappendePerioder(perioder).map {
                PeriodeMedVerdi(
                    periode = it,
                    verdi = bt.verdi,
                )
            }
        }.tilPeriodisering()

        return this.copy(periodisering = krympetPeriodisering)
    }

    companion object {

        fun periodiserOgFyllUtHullMed0(
            perioder: BarnetilleggPerioder,
            begrunnelse: Begrunnelse?,
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
private fun BarnetilleggPerioder.periodiserOgFyllUtHullMed0(virkningsperiode: Periode): Periodisering<AntallBarn> {
    if (this.map { it.first }.inneholderOverlapp()) {
        throw IllegalArgumentException("Periodene kan ikke overlappe")
    }
    return this.tilPeriodisering().utvid(AntallBarn.ZERO, virkningsperiode) as SammenhengendePeriodisering
}
