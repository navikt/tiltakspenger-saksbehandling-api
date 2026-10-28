package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.toÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ÅpenPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import java.time.LocalDate

data class TiltaksdeltakelseMedArrangørnavnDTO(
    val eksternDeltakelseId: String,
    val typeKode: String?,
    val fratarRett: Boolean,
    val typeNavn: String,
    @Deprecated("Bruk periode. Beholdes for kompatibilitet.")
    val deltakelseFraOgMed: LocalDate?,
    @Deprecated("Bruk periode. Beholdes for kompatibilitet.")
    val deltakelseTilOgMed: LocalDate?,
    val periode: ÅpenPeriodeDTO,
    val visningsnavn: SladdbarVerdi<String>,
    val status: TiltakDeltakerstatusDto,
)

fun TiltaksdeltakelseMedArrangørnavn.toDTO(): TiltaksdeltakelseMedArrangørnavnDTO {
    return TiltaksdeltakelseMedArrangørnavnDTO(
        eksternDeltakelseId = this.eksternDeltakelseId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode?.name,
        fratarRett = this.fratarRett,
        deltakelseFraOgMed = this.periode.fraOgMed,
        deltakelseTilOgMed = this.periode.tilOgMed,
        periode = this.periode.toÅpenPeriodeDTO(),
        visningsnavn = this.visningsnavn.ikkeSladdet(),
        status = this.status.toDto(),
    )
}
