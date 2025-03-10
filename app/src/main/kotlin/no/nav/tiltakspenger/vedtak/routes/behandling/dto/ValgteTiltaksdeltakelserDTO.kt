package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO

data class ValgteTiltaksdeltakelserDTO(
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
)

data class TiltaksdeltakelsePeriodeDTO(
    val eksternDeltagelseId: String,
    val periode: PeriodeDTO,
)
