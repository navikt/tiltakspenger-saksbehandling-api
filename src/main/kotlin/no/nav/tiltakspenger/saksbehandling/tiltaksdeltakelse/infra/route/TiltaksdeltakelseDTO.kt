package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import java.time.LocalDate

data class TiltaksdeltakelseDTO(
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: String,
    val deltagelseFraOgMed: LocalDate?,
    val deltagelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatusDto,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
    val gjennomforingsprosent: Float?,
    val internDeltakelseId: String,
)

fun TiltaksdeltakelseIntern.toDTO(): TiltaksdeltakelseDTO {
    return TiltaksdeltakelseDTO(
        eksternDeltagelseId = this.eksternDeltakelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.name,
        deltagelseFraOgMed = this.deltakelseFraOgMed,
        deltagelseTilOgMed = this.deltakelseTilOgMed,
        deltakelseStatus = this.deltakelseStatus.toDto(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
        gjennomforingsprosent = this.deltidsprosentGjennomforing?.toFloat(),
        internDeltakelseId = this.internDeltakelseId.toString(),
    )
}
