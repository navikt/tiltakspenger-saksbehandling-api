package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import java.time.LocalDate

/**
 * Inneholder verken arrangør eller tittel, og trenger derfor ikke sladding.
 */
data class TiltaksdeltakelseSomFratarRettDTO(
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

fun TiltaksdeltakelseSomFratarRett.toDTO(): TiltaksdeltakelseSomFratarRettDTO {
    return TiltaksdeltakelseSomFratarRettDTO(
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
