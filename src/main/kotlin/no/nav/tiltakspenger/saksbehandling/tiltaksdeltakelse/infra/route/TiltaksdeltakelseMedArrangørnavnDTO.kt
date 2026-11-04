package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.toÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn

data class TiltaksdeltakelseMedArrangørnavnDTO(
    val eksternDeltakelseId: String,
    val tiltakskodeFraKilden: String,
    val fratarRett: Boolean,
    val typeNavn: String,
    val periode: ÅpenPeriodeDTO,
    val visningsnavn: SladdbarVerdi<String>,
    val status: TiltakDeltakerstatusDto,
)

fun TiltaksdeltakelseMedArrangørnavn.toDTO(): TiltaksdeltakelseMedArrangørnavnDTO {
    return TiltaksdeltakelseMedArrangørnavnDTO(
        eksternDeltakelseId = this.eksternDeltakelseId,
        typeNavn = this.typeNavn,
        tiltakskodeFraKilden = this.tiltakskodeFraKilden,
        fratarRett = this.fratarRett,
        periode = this.periode.toÅpenPeriodeDTO(),
        visningsnavn = this.visningsnavn.ikkeSladdet(),
        status = this.status.toDto(),
    )
}
