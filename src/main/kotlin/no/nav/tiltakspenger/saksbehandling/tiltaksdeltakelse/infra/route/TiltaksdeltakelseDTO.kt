package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.toÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern

data class TiltaksdeltakelseDTO(
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val periode: ÅpenPeriodeDTO,
    val deltakelseStatus: TiltakDeltakerstatusDto,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
    val gjennomforingsprosent: Float?,
    val internDeltakelseId: String,
    val kanInnvilges: Boolean,
)

fun TiltaksdeltakelseIntern.toDTO(): TiltaksdeltakelseDTO {
    return TiltaksdeltakelseDTO(
        eksternDeltagelseId = this.eksternDeltakelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        periode = this.periode.toÅpenPeriodeDTO(),
        deltakelseStatus = this.deltakelseStatus.toDto(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
        gjennomforingsprosent = this.deltidsprosentGjennomforing?.toFloat(),
        internDeltakelseId = this.internDeltakelseId.toString(),
        kanInnvilges = this.kanInnvilges,
    )
}
