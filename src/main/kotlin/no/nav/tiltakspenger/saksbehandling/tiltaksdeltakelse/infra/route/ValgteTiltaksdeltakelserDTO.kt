package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

data class TiltaksdeltakelsePeriodeDTO(
    val eksternDeltagelseId: String,
    val periode: PeriodeDTO,
)

fun PeriodeMedVerdi<Tiltaksdeltakelse>.toTiltaksdeltakelsePeriodeDTO() = TiltaksdeltakelsePeriodeDTO(
    eksternDeltagelseId = verdi.eksternDeltagelseId,
    periode = periode.toDTO(),
)
