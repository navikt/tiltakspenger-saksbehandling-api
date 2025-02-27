package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering

internal data class BarnetilleggDTO(
    val perioder: List<BarnetilleggPeriodeDTO>,
    val begrunnelse: String?,
) {
    fun tilBarnetillegg(): Barnetillegg = Barnetillegg(
        begrunnelse = begrunnelse?.let { (BegrunnelseVilkårsvurdering(it)) },
        value = Periodisering(
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
    perioder = value.perioderMedVerdi.map { it.tilBarnetilleggPeriodeDTO() },
    begrunnelse = begrunnelse?.verdi,
)

internal fun PeriodeMedVerdi<AntallBarn>.tilBarnetilleggPeriodeDTO(): BarnetilleggPeriodeDTO = BarnetilleggPeriodeDTO(
    antallBarn = verdi.value,
    periode = periode.toDTO(),
)
