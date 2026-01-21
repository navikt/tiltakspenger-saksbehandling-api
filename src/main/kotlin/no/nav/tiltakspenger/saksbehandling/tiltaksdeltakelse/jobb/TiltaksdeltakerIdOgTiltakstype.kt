package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.jobb

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

data class TiltaksdeltakerIdOgTiltakstype(
    val id: TiltaksdeltakerId,
    val tiltakstype: TiltakResponsDTO.TiltakType,
)
