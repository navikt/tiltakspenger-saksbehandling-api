package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger

data class SaksopplysningerDTO(
    val fødselsdato: String,
    val tiltaksdeltagelse: List<TiltaksdeltagelseDTO>,
)

fun no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger.toSaksopplysningerDTO(): SaksopplysningerDTO {
    return SaksopplysningerDTO(
        fødselsdato = this.fødselsdato.toString(),
        tiltaksdeltagelse = this.tiltaksdeltagelse.map { it.toDTO() },
    )
}
