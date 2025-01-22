package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.institusjonsopphold

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.Opphold

data class PeriodeMedOppholdDTO(
    val periode: PeriodeDTO,
    val opphold: OppholdDTO,
)

enum class OppholdDTO {
    OPPHOLD,
    IKKE_OPPHOLD,
}

fun Opphold.toDTO(): OppholdDTO = OppholdDTO.valueOf(this.name)

fun PeriodeMedVerdi<Opphold>.toDTO(): PeriodeMedOppholdDTO =
    PeriodeMedOppholdDTO(
        periode = this.periode.toDTO(),
        opphold = this.verdi.toDTO(),
    )
