package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse

data class PeriodeMedDeltagelseDTO(
    val periode: PeriodeDTO,
    val deltagelse: DeltagelseDTO,
)

enum class DeltagelseDTO {
    DELTAR,
    DELTAR_IKKE,
}

fun Deltagelse.toDTO(): DeltagelseDTO = DeltagelseDTO.valueOf(this.name)

fun PeriodeMedVerdi<Deltagelse>.toDTO(): PeriodeMedDeltagelseDTO =
    PeriodeMedDeltagelseDTO(
        periode = this.periode.toDTO(),
        deltagelse = this.verdi.toDTO(),
    )
