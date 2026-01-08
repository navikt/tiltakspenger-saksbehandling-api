package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDate

data class TiltaksdeltakelseDTO(
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
    val gjennomforingsprosent: Float?,
    val internDeltakelseId: String?,
)

fun Tiltaksdeltakelse.toDTO(): TiltaksdeltakelseDTO {
    return TiltaksdeltakelseDTO(
        eksternDeltagelseId = this.eksternDeltakelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.name,
        deltagelseFraOgMed = this.deltakelseFraOgMed,
        deltagelseTilOgMed = this.deltakelseTilOgMed,
        deltakelseStatus = this.deltakelseStatus.name,
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
        gjennomforingsprosent = this.deltidsprosentGjennomforing?.toFloat(),
        internDeltakelseId = this.internDeltakelseId,
    )
}
