package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomIkkeGirRett
import java.time.LocalDate

/**
 * Inneholder verken arrangør eller tittel, og trenger derfor ikke sladding.
 */
data class TiltaksdeltakelseSomIkkeGirRettDTO(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val tiltakskodeFraKilden: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatusDto,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
)

fun TiltaksdeltakelseSomIkkeGirRett.toDTO(): TiltaksdeltakelseSomIkkeGirRettDTO {
    return TiltaksdeltakelseSomIkkeGirRettDTO(
        eksternDeltakelseId = this.eksternDeltakelseId,
        typeNavn = this.typeNavn,
        tiltakskodeFraKilden = this.tiltakskodeFraKilden,
        deltakelseFraOgMed = this.deltakelseFraOgMed,
        deltakelseTilOgMed = this.deltakelseTilOgMed,
        deltakelseStatus = this.deltakelseStatus.toDto(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
    )
}
