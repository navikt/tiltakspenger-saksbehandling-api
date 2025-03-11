package no.nav.tiltakspenger.saksbehandling.routes.behandling.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.ValgteTiltaksdeltakelser

data class ValgteTiltaksdeltakelserDTO(
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
)

data class TiltaksdeltakelsePeriodeDTO(
    val eksternDeltagelseId: String,
    val periode: PeriodeDTO,
)

fun ValgteTiltaksdeltakelser.toDTO() = ValgteTiltaksdeltakelserDTO(
    valgteTiltaksdeltakelser = periodisering.perioderMedVerdi.map { it.toTiltaksdeltakelsePeriodeDTO() },
)

fun PeriodeMedVerdi<Tiltaksdeltagelse>.toTiltaksdeltakelsePeriodeDTO() = TiltaksdeltakelsePeriodeDTO(
    eksternDeltagelseId = verdi.eksternDeltagelseId,
    periode = periode.toDTO(),
)
