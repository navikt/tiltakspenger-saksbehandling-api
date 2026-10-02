package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.LocalDate

data class AutomatiskOpprettetRevurderingGrunnDTO(
    val endringer: List<TiltaksdeltakerEndringDTO>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.AvsluttetSomForventet::class, name = "AVSLUTTET_SOM_FORVENTET"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.AvbruttDeltakelse::class, name = "AVBRUTT_DELTAKELSE"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.IkkeAktuellDeltakelse::class, name = "IKKE_AKTUELL_DELTAKELSE"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.Forlengelse::class, name = "FORLENGELSE"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.EndretSluttdato::class, name = "ENDRET_SLUTTDATO"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.EndretStartdato::class, name = "ENDRET_STARTDATO"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.EndretDeltakelsesmengde::class, name = "ENDRET_DELTAKELSESMENGDE"),
    JsonSubTypes.Type(value = TiltaksdeltakerEndringDTO.EndretStatus::class, name = "ENDRET_STATUS"),
)
sealed interface TiltaksdeltakerEndringDTO {

    data object AvsluttetSomForventet : TiltaksdeltakerEndringDTO

    data object AvbruttDeltakelse : TiltaksdeltakerEndringDTO

    data object IkkeAktuellDeltakelse : TiltaksdeltakerEndringDTO

    data class Forlengelse(val nySluttdato: LocalDate) : TiltaksdeltakerEndringDTO

    data class EndretSluttdato(val nySluttdato: LocalDate?) : TiltaksdeltakerEndringDTO

    data class EndretStartdato(val nyStartdato: LocalDate?) : TiltaksdeltakerEndringDTO

    data class EndretDeltakelsesmengde(
        val nyDeltakelsesprosent: Float?,
        val nyDagerPerUke: Float?,
    ) : TiltaksdeltakerEndringDTO

    data class EndretStatus(val nyStatus: String) : TiltaksdeltakerEndringDTO
}

/**
 * Endringen sendes som en liste av enkeltendringer, slik frontend forventer.
 */
fun AutomatiskOpprettetRevurderingGrunn.toDTO(): AutomatiskOpprettetRevurderingGrunnDTO {
    return AutomatiskOpprettetRevurderingGrunnDTO(
        endringer = endring.toDTO(),
    )
}

private fun TiltaksdeltakerEndring.toDTO(): List<TiltaksdeltakerEndringDTO> = when (this) {
    is TiltaksdeltakerEndring.AvsluttetSomForventet -> listOf(TiltaksdeltakerEndringDTO.AvsluttetSomForventet)

    is TiltaksdeltakerEndring.AvbruttDeltakelse -> listOf(TiltaksdeltakerEndringDTO.AvbruttDeltakelse)

    is TiltaksdeltakerEndring.IkkeAktuellDeltakelse -> listOf(TiltaksdeltakerEndringDTO.IkkeAktuellDeltakelse)

    is TiltaksdeltakerEndring.Forlengelse -> listOfNotNull(
        endretDeltakelsesmengde?.toDTO(),
        TiltaksdeltakerEndringDTO.Forlengelse(nySluttdato = nySluttdato),
    )

    is TiltaksdeltakerEndring.AndreEndringer -> endringer.map { it.toDTO() }
}

private fun TiltaksdeltakerEndring.Endringsdetalj.toDTO(): TiltaksdeltakerEndringDTO = when (this) {
    is TiltaksdeltakerEndring.EndretDeltakelsesmengde -> TiltaksdeltakerEndringDTO.EndretDeltakelsesmengde(nyDeltakelsesprosent = nyDeltakelsesprosent, nyDagerPerUke = nyDagerPerUke)
    is TiltaksdeltakerEndring.EndretStartdato -> TiltaksdeltakerEndringDTO.EndretStartdato(nyStartdato = nyStartdato)
    is TiltaksdeltakerEndring.EndretSluttdato -> TiltaksdeltakerEndringDTO.EndretSluttdato(nySluttdato = nySluttdato)
    is TiltaksdeltakerEndring.EndretStatus -> TiltaksdeltakerEndringDTO.EndretStatus(nyStatus = nyStatus.name)
}
