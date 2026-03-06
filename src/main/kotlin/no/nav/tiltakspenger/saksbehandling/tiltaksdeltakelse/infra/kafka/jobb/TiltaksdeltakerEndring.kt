package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import java.time.LocalDate

sealed interface TiltaksdeltakerEndring {
    val beskrivelse: String

    data object AvbruttDeltakelse : TiltaksdeltakerEndring {
        override val beskrivelse = "Deltakelsen er avbrutt"
    }

    data object IkkeAktuellDeltakelse : TiltaksdeltakerEndring {
        override val beskrivelse = "Deltakelsen er ikke aktuell"
    }

    data class Forlengelse(val nySluttdato: LocalDate) : TiltaksdeltakerEndring {
        override val beskrivelse = "Deltakelsen har blitt forlenget"
    }

    data class EndretSluttdato(val nySluttdato: LocalDate?) : TiltaksdeltakerEndring {
        override val beskrivelse = "Endret sluttdato"
    }

    data class EndretStartdato(val nyStartdato: LocalDate?) : TiltaksdeltakerEndring {
        override val beskrivelse = "Endret startdato"
    }

    data class EndretDeltakelsesmengde(val nyDeltakelsesprosent: Float?, val nyDagerPerUke: Float?) : TiltaksdeltakerEndring {
        override val beskrivelse = "Endret deltakelsesmengde"
    }

    data class EndretStatus(val nyStatus: TiltakDeltakerstatus) : TiltaksdeltakerEndring {
        override val beskrivelse = "Endret status"
    }
}

fun List<TiltaksdeltakerEndring>.getOppgaveTilleggstekst(): String? {
    if (this.isEmpty()) {
        return null
    }
    if (this.size == 1) {
        return "${this.first().beskrivelse}."
    }

    return this.joinToString("\n") { "- ${it.beskrivelse}" }
}
