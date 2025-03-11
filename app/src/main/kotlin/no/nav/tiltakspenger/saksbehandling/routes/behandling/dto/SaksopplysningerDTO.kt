package no.nav.tiltakspenger.saksbehandling.routes.behandling.dto

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger

data class SaksopplysningerDTO(
    val fødselsdato: String,
    val tiltaksdeltagelse: List<TiltaksdeltagelseDTO>,
)

fun Saksopplysninger.toDTO(): SaksopplysningerDTO {
    return SaksopplysningerDTO(
        fødselsdato = this.fødselsdato.toString(),
        tiltaksdeltagelse = this.tiltaksdeltagelse.map { it.toDTO() },
    )
}
