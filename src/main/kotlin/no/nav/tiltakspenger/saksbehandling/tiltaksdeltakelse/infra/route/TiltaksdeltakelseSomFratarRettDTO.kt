package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.toÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import java.time.LocalDate

/**
 * Inneholder verken arrangør eller tittel, og trenger derfor ikke sladding.
 */
data class TiltaksdeltakelseSomFratarRettDTO(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val tiltakskodeFraKilden: String,
    @Deprecated("Bruk periode. Beholdes for kompatibilitet.")
    val deltakelseFraOgMed: LocalDate?,
    @Deprecated("Bruk periode. Beholdes for kompatibilitet.")
    val deltakelseTilOgMed: LocalDate?,
    val periode: ÅpenPeriodeDTO,
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
        deltakelseFraOgMed = this.periode.fraOgMed,
        deltakelseTilOgMed = this.periode.tilOgMed,
        periode = this.periode.toÅpenPeriodeDTO(),
        deltakelseStatus = this.deltakelseStatus.toDto(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
    )
}
