package no.nav.tiltakspenger.vedtak.routes.behandling

import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import java.time.LocalDate

data class TiltaksdeltagelseDTO(
    val id: String,
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: String,
    val deltagelseFraOgMed: LocalDate?,
    val deltagelseTilOgMed: LocalDate?,
    val deltakelseStatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
)

fun Tiltaksdeltagelse.toDTO(): TiltaksdeltagelseDTO {
    return TiltaksdeltagelseDTO(
        id = this.id.toString(),
        eksternDeltagelseId = this.eksternDeltagelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.name,
        deltagelseFraOgMed = this.deltakelsesperiode.fraOgMed,
        deltagelseTilOgMed = this.deltakelsesperiode.tilOgMed,
        deltakelseStatus = this.deltakelseStatus.name,
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
    )
}
