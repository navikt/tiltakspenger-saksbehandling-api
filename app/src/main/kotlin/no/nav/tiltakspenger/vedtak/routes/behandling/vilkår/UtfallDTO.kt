package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode

internal data class PeriodisertUtfallDTO(
    val utfall: UtfallDTO,
    val periode: PeriodeDTO,
) {
    enum class UtfallDTO {
        OPPFYLT,
        IKKE_OPPFYLT,
        UAVKLART,
    }
}

internal fun Periodisering<UtfallForPeriode>.toDTO(): List<PeriodisertUtfallDTO> =
    this.perioderMedVerdi.map {
        PeriodisertUtfallDTO(
            utfall = it.verdi.toDTO(),
            periode = it.periode.toDTO(),
        )
    }

internal fun UtfallForPeriode.toDTO(): PeriodisertUtfallDTO.UtfallDTO =
    when (this) {
        UtfallForPeriode.OPPFYLT -> PeriodisertUtfallDTO.UtfallDTO.OPPFYLT
        UtfallForPeriode.IKKE_OPPFYLT -> PeriodisertUtfallDTO.UtfallDTO.IKKE_OPPFYLT
        UtfallForPeriode.UAVKLART -> PeriodisertUtfallDTO.UtfallDTO.UAVKLART
    }
