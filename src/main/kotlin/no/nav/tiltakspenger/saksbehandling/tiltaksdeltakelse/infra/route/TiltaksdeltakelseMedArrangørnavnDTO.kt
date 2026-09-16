package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import java.time.LocalDate

data class TiltaksdeltakelseMedArrangørnavnDTO(
    val eksternDeltakelseId: String,
    val typeKode: String,
    val typeNavn: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val visningsnavn: SladdbarVerdi<String>,
    val status: TiltakDeltakerstatusDto,
)

fun TiltaksdeltakelseMedArrangørnavn.toDTO(): TiltaksdeltakelseMedArrangørnavnDTO {
    return TiltaksdeltakelseMedArrangørnavnDTO(
        eksternDeltakelseId = this.eksternDeltakelseId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.name,
        deltakelseFraOgMed = this.deltakelseFraOgMed,
        deltakelseTilOgMed = this.deltakelseTilOgMed,
        visningsnavn = this.visningsnavn.ikkeSladdet(),
        status = this.status.toDto(),
    )
}
