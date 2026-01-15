package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.perioder
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import org.jetbrains.annotations.TestOnly

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val periodisering: IkkeTomPeriodisering<AntallBarn>,
    val begrunnelse: Begrunnelse?,
) {
    val harBarnetillegg: Boolean by lazy {
        periodisering.any { it.verdi != AntallBarn.ZERO }
    }

    fun krympTilPerioder(perioder: List<Periode>): Barnetillegg? {
        val krympetPeriodisering = periodisering.perioderMedVerdi.toList().flatMap { bt ->
            bt.periode.overlappendePerioder(perioder).map {
                PeriodeMedVerdi(
                    periode = it,
                    verdi = bt.verdi,
                )
            }
        }

        if (krympetPeriodisering.isEmpty()) {
            return null
        }

        return this.copy(periodisering = krympetPeriodisering.tilIkkeTomPeriodisering())
    }

    companion object {

        /** Fyller hullene innenfor innvilgelsesperiodene med 0 barn, men ikke evt hull mellom innvilgelsesperiodene */
        fun periodiserOgFyllUtHullMed0(
            perioderMedBarn: NonEmptyList<Pair<Periode, AntallBarn>>,
            begrunnelse: Begrunnelse?,
            innvilgelsesperioder: NonEmptyList<Periode>,
        ): Barnetillegg {
            val perioderMedBarnetillegg = perioderMedBarn.map {
                PeriodeMedVerdi(
                    verdi = it.second,
                    periode = it.first,
                )
            }

            val perioder = perioderMedBarnetillegg.perioder()
            val perioderUtenInnvilgelse = perioder.trekkFra(innvilgelsesperioder)

            require(perioderUtenInnvilgelse.isEmpty()) {
                "Barnetilleggsperiodene må være innenfor innvilgelsesperiodene"
            }

            val perioderUtenBarnetillegg = innvilgelsesperioder.trekkFra(perioder).map {
                PeriodeMedVerdi(
                    verdi = AntallBarn.ZERO,
                    periode = it,
                )
            }

            return Barnetillegg(
                periodisering = perioderMedBarnetillegg.plus(perioderUtenBarnetillegg)
                    .sortedBy { it.periode.fraOgMed }
                    .tilIkkeTomPeriodisering(),
                begrunnelse = begrunnelse,
            )
        }

        fun utenBarnetillegg(perioder: NonEmptyList<Periode>): Barnetillegg {
            return Barnetillegg(
                periodisering = perioder.map {
                    PeriodeMedVerdi(
                        periode = it,
                        verdi = AntallBarn.ZERO,
                    )
                }.tilIkkeTomPeriodisering(),
                begrunnelse = null,
            )
        }

        @TestOnly
        fun utenBarnetillegg(periode: Periode): Barnetillegg {
            return utenBarnetillegg(nonEmptyListOf(periode))
        }
    }
}
