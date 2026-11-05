package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Første versjon av barnetillegg, der saksbehandler satte antall barn per periode uten å oppgi hvilke barn det gjaldt.
 * Lagres ikke lenger fra saksbehandler, men finnes på eldre behandlinger og vedtak, og i forlengelser av dem.
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
}
