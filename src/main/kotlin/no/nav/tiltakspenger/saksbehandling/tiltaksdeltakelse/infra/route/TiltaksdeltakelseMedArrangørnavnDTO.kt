package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import java.time.LocalDate

data class TiltaksdeltakelseMedArrangørnavnDTO(
    val eksternDeltakelseId: String,
    val harAdressebeskyttelse: Boolean,
    val gjennomføringId: String?,
    val typeNavn: String?,
    val typeKode: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
)

fun TiltaksdeltakelseMedArrangørnavn.toDTO(): TiltaksdeltakelseMedArrangørnavnDTO {
    return TiltaksdeltakelseMedArrangørnavnDTO(
        eksternDeltakelseId = this.eksternDeltakelseId,
        harAdressebeskyttelse = this.harAdressebeskyttelse,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.name,
        deltakelseFraOgMed = this.deltakelseFraOgMed,
        deltakelseTilOgMed = this.deltakelseTilOgMed,
        deltakelseStatus = this.deltakelseStatus.name,
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.name,
    )
}
