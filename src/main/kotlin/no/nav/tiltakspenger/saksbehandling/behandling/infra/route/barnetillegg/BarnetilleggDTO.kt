package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.periodiserOgFyllUtHullMed0
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering

data class BarnetilleggDTO(
    val perioder: List<BarnetilleggPeriodeDTO>,
    val begrunnelse: String?,
) {
    fun tilBarnetillegg(virkningsperiode: Periode?): Barnetillegg = Barnetillegg.periodiserOgFyllUtHullMed0(
        begrunnelse = begrunnelse?.let { (BegrunnelseVilkårsvurdering(saniter(it))) },
        perioder = perioder.map { Pair(it.periode.toDomain(), AntallBarn(it.antallBarn)) },
        virkningsperiode = virkningsperiode,
    )
}

data class BarnetilleggPeriodeDTO(
    val antallBarn: Int,
    val periode: PeriodeDTO,
)

fun Barnetillegg.toBarnetilleggDTO(): BarnetilleggDTO = BarnetilleggDTO(
    perioder = periodisering.perioderMedVerdi.filter { it.verdi.value > 0 }.map {
        BarnetilleggPeriodeDTO(
            antallBarn = it.verdi.value,
            periode = it.periode.toDTO(),
        )
    },
    begrunnelse = begrunnelse?.verdi?.let { saniter(it) },
)

fun List<BarnetilleggPeriodeDTO>.tilPeriodisering(virkningsperiode: Periode?) =
    this.map { Pair(it.periode.toDomain(), AntallBarn(it.antallBarn)) }
        .periodiserOgFyllUtHullMed0(virkningsperiode)
