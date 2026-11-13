package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.perioder
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Første versjon av barnetillegg, der saksbehandler satte antall barn per periode uten å oppgi hvilke barn det gjaldt.
 * Finnes på eldre behandlinger og vedtak, og i forlengelser og omgjøringer av dem.
 * Kan ikke migreres til [BarnetilleggV2], siden vi ikke vet hvilke barn antallet gjelder.
 */
data class BarnetilleggV1(
    val periodisering: IkkeTomPeriodisering<AntallBarn>,
    override val begrunnelse: Begrunnelse?,
) : Barnetillegg {
    /** Antallet er lagret per periode, så [innvilgelsesperioder] brukes ikke. */
    override fun periodisering(innvilgelsesperioder: NonEmptyList<Periode>): IkkeTomPeriodisering<AntallBarn> = periodisering

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
}
