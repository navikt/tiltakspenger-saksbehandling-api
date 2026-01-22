package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

data class Tiltaksdeltaker(
    val id: TiltaksdeltakerId,
    val eksternId: String,
    val tiltakstype: TiltakResponsDTO.TiltakType,
    val utdatertEksternId: String?,
)
