package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import java.time.LocalDate

/**
 * Utfallet av å sammenligne nå-tilstanden til en tiltaksdeltakelse med tilstanden saken kjenner til.
 * Hver endring av deltakelsen tolkes til nøyaktig ett utfall, slik at det ikke må tolkes på nytt for å avgjøre hvilken revurdering som skal opprettes.
 */
sealed interface TiltaksdeltakerEndring {

    /** Beskrivelsene av endringene, i den rekkefølgen de vises i oppgaveteksten. */
    val beskrivelser: List<String>

    data object AvsluttetSomForventet : TiltaksdeltakerEndring {
        override val beskrivelser = listOf("Deltakelsen er avsluttet som forventet")
    }

    data object AvbruttDeltakelse : TiltaksdeltakerEndring {
        override val beskrivelser = listOf("Deltakelsen er avbrutt")
    }

    data object IkkeAktuellDeltakelse : TiltaksdeltakerEndring {
        override val beskrivelser = listOf("Deltakelsen er ikke aktuell")
    }

    /**
     * Sluttdatoen er flyttet frem, med samme startdato.
     * [endretDeltakelsesmengde] er satt dersom deltakelsesmengden er endret samtidig.
     */
    data class Forlengelse(
        val nySluttdato: LocalDate,
        val endretDeltakelsesmengde: EndretDeltakelsesmengde? = null,
    ) : TiltaksdeltakerEndring {
        override val beskrivelser = listOfNotNull(endretDeltakelsesmengde?.beskrivelse, "Deltakelsen har blitt forlenget")
    }

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

        override val beskrivelser = endringer.map { it.beskrivelse }
    }

    sealed interface Endringsdetalj {
        val beskrivelse: String
    }

    data class EndretDeltakelsesmengde(val nyDeltakelsesprosent: Float?, val nyDagerPerUke: Float?) : Endringsdetalj {
        override val beskrivelse = "Endret deltakelsesmengde"
    }

    data class EndretStartdato(val nyStartdato: LocalDate?) : Endringsdetalj {
        override val beskrivelse = "Endret startdato"
    }

    data class EndretSluttdato(val nySluttdato: LocalDate?) : Endringsdetalj {
        override val beskrivelse = "Endret sluttdato"
    }

    data class EndretStatus(val nyStatus: TiltakDeltakerstatus) : Endringsdetalj {
        override val beskrivelse = "Endret status"
    }

    fun getOppgaveTilleggstekst(): String =
        if (beskrivelser.size == 1) "${beskrivelser.first()}." else beskrivelser.joinToString("\n") { "- $it" }
}
