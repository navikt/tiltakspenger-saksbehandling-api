package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.toÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett

data class TiltaksdeltakelseSomFratarRettDTO(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val tittel: SladdbarVerdi<String?>,
    val tiltakskodeFraKilden: String,
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
        tittel = this.tittel.ikkeSladdet(),
        tiltakskodeFraKilden = this.tiltakskodeFraKilden,
        periode = this.periode.toÅpenPeriodeDTO(),
        deltakelseStatus = this.deltakelseStatus.toDto(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
    )
}
