package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import java.time.LocalDate

/**
 * Utfallet av å sammenligne nå-tilstanden til en tiltaksdeltakelse med tilstanden saken kjenner til.
 * Hver endring av deltakelsen tolkes til nøyaktig ett utfall, slik at det ikke må tolkes på nytt for å avgjøre hvilken revurdering som skal opprettes.
 */
sealed interface TiltaksdeltakerEndring {

    /** Om endringen kan påvirke saken og krever oppfølging. */
    val erRelevant: Boolean
        get() = when (this) {
            AvsluttetSomForventet -> false

            AvbruttDeltakelse,
            IkkeAktuellDeltakelse,
            is Forlengelse,
            is Avkortet,
            is AndreEndringer,
            -> true
        }

    data object AvsluttetSomForventet : TiltaksdeltakerEndring

    /**
     * Deltakelsen har fått status avbrutt, eller sluttdatoen er avkortet til en dato som er passert uten at deltakelsen har status deltar.
     * Startdatoen og deltakelsesmengden er uendret, ellers er det i stedet [AndreEndringer], slik at de ikke går tapt i en stans eller et opphør.
     */
    data object AvbruttDeltakelse : TiltaksdeltakerEndring

    data object IkkeAktuellDeltakelse : TiltaksdeltakerEndring

    /**
     * Sluttdatoen er flyttet frem, med samme startdato.
     * [endretDeltakelsesmengde] er satt dersom deltakelsesmengden er endret samtidig.
     */
    data class Forlengelse(
        val nySluttdato: LocalDate,
        val endretDeltakelsesmengde: EndretDeltakelsesmengde? = null,
    ) : TiltaksdeltakerEndring

    /**
     * Sluttdatoen er flyttet til en dato før siste innvilgede dag, men den er ikke passert ennå, og deltakelsen har fortsatt status deltar.
     * Startdatoen og deltakelsesmengden er uendret.
     * Er sluttdatoen passert, er deltakelsen i stedet regnet som [AvbruttDeltakelse].
     */
    data class Avkortet(val nySluttdato: LocalDate) : TiltaksdeltakerEndring

    /**
     * Endringer som ikke går under noen av de andre utfallene.
     * Minst én av endringene er satt.
     */
    data class AndreEndringer(
        val endretDeltakelsesmengde: EndretDeltakelsesmengde? = null,
        val endretStartdato: EndretStartdato? = null,
        val endretSluttdato: EndretSluttdato? = null,
        val endretStatus: EndretStatus? = null,
    ) : TiltaksdeltakerEndring {
        val endringer: List<Endringsdetalj> = listOfNotNull(endretDeltakelsesmengde, endretStartdato, endretSluttdato, endretStatus)

        init {
            require(endringer.isNotEmpty()) { "AndreEndringer må ha minst én endring" }
        }
    }

    sealed interface Endringsdetalj

    data class EndretDeltakelsesmengde(val nyDeltakelsesprosent: Float?, val nyDagerPerUke: Float?) : Endringsdetalj

    data class EndretStartdato(val nyStartdato: LocalDate?) : Endringsdetalj

    data class EndretSluttdato(val nySluttdato: LocalDate?) : Endringsdetalj

    data class EndretStatus(val nyStatus: TiltakDeltakerstatus) : Endringsdetalj
}
