package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltagelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO

data class SaksopplysningerDTO(
    val fødselsdato: String,
    val tiltaksdeltagelse: List<TiltaksdeltagelseDTO>,
    val periode: PeriodeDTO,
)

fun Saksopplysninger.toSaksopplysningerDTO(): SaksopplysningerDTO {
    return SaksopplysningerDTO(
        fødselsdato = this.fødselsdato.toString(),
        tiltaksdeltagelse = this.tiltaksdeltagelse.map { it.toDTO() },
        periode = this.periode.toDTO(),
    )
}
