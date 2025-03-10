package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.vedtak.barnetillegg.AntallBarn
import no.nav.tiltakspenger.vedtak.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering

internal data class BarnetilleggDTO(
    val perioder: List<BarnetilleggPeriodeDTO>,
    val begrunnelse: String?,
) {
    fun tilBarnetillegg(): Barnetillegg = Barnetillegg(
        begrunnelse = begrunnelse?.let { (BegrunnelseVilkårsvurdering(it)) },
        periodisering = Periodisering(
            perioder.map {
                PeriodeMedVerdi(AntallBarn(it.antallBarn), it.periode.toDomain())
            },
        ),
    )
}

internal data class BarnetilleggPeriodeDTO(
    val antallBarn: Int,
    val periode: PeriodeDTO,
)

internal fun Barnetillegg.toDTO(): BarnetilleggDTO = BarnetilleggDTO(
    perioder = periodisering.perioderMedVerdi.map { it.tilBarnetilleggPeriodeDTO() },
    begrunnelse = begrunnelse?.verdi,
)

internal fun PeriodeMedVerdi<AntallBarn>.tilBarnetilleggPeriodeDTO(): BarnetilleggPeriodeDTO = BarnetilleggPeriodeDTO(
    antallBarn = verdi.value,
    periode = periode.toDTO(),
)
