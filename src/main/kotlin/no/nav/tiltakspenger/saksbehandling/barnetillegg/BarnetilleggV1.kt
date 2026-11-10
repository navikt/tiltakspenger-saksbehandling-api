package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.trekkFra
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.perioder
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Første versjon av barnetillegg, der saksbehandler satte antall barn per periode uten å oppgi hvilke barn det gjaldt.
 * Finnes på eldre behandlinger og vedtak, og i forlengelser av dem.
 * Lagres ellers bare fra eldre versjoner av frontenden, mens den rulles ut, se [create].
 * Kan ikke migreres til [BarnetilleggV2], siden vi ikke vet hvilke barn antallet gjelder.
 */
data class BarnetilleggV1(
    override val periodisering: IkkeTomPeriodisering<AntallBarn>,
    override val begrunnelse: Begrunnelse?,
) : Barnetillegg {
    override fun krympTilPerioder(perioder: List<Periode>): BarnetilleggV1? {
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
        /**
         * Lager barnetillegget fra antall barn per periode, slik eldre versjoner av frontenden sender det.
         * Dager i innvilgelsesperiodene uten en periode får 0 barn, men hull mellom innvilgelsesperiodene fylles ikke.
         *
         * TODO: Fjernes når alle saksbehandlere har frontenden som sender barna, se [BarnetilleggV2].
         */
        fun create(
            perioderMedBarn: NonEmptyList<Pair<Periode, AntallBarn>>,
            innvilgelsesperioder: NonEmptyList<Periode>,
            begrunnelse: Begrunnelse?,
        ): Either<UgyldigBarnetillegg, BarnetilleggV1> {
            val perioderMedBarnetillegg = perioderMedBarn.map { (periode, antallBarn) ->
                PeriodeMedVerdi(verdi = antallBarn, periode = periode)
            }
            val perioder = perioderMedBarnetillegg.perioder()

            if (perioder.trekkFra(innvilgelsesperioder).isNotEmpty()) {
                return UgyldigBarnetillegg.AntallBarnUtenforInnvilgelsen.left()
            }

            val perioderUtenBarnetillegg = innvilgelsesperioder.trekkFra(perioder).map {
                PeriodeMedVerdi(verdi = AntallBarn.ZERO, periode = it)
            }

            return BarnetilleggV1(
                periodisering = perioderMedBarnetillegg.plus(perioderUtenBarnetillegg)
                    .sortedBy { it.periode.fraOgMed }
                    .tilIkkeTomPeriodisering(),
                begrunnelse = begrunnelse,
            ).right()
        }
    }
}
