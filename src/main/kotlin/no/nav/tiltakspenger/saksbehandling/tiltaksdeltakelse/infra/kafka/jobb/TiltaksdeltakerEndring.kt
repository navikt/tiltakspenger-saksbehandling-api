package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring.AvbruttDeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring.EndretDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring.EndretSluttdato
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring.EndretStartdato
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring.Forlengelse
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

data class TiltaksdeltakerEndringer(
    val endringer: NonEmptyList<TiltaksdeltakerEndring>,
) : List<TiltaksdeltakerEndring> by endringer {

    val avbrutt: AvbruttDeltakelse? by lazy { filterIsInstance<AvbruttDeltakelse>().firstOrNull() }

    val forlengelse: Forlengelse? by lazy { filterIsInstance<Forlengelse>().firstOrNull() }

    val endretStartdato: EndretStartdato? by lazy { filterIsInstance<EndretStartdato>().firstOrNull() }

    val endretSluttdato: EndretSluttdato? by lazy { filterIsInstance<EndretSluttdato>().firstOrNull() }

    val endretDeltakelsesmengde: EndretDeltakelsesmengde? by lazy { filterIsInstance<EndretDeltakelsesmengde>().firstOrNull() }

    fun getOppgaveTilleggstekst(): String? {
        if (this.isEmpty()) {
            return null
        }

        if (this.size == 1) {
            return "${this.first().beskrivelse}."
        }

        return this.joinToString("\n") { "- ${it.beskrivelse}" }
    }

    companion object {
        fun List<TiltaksdeltakerEndring>.tilEndringer(): TiltaksdeltakerEndringer? {
            return this.toNonEmptyListOrNull()?.let { TiltaksdeltakerEndringer(it) }
        }
    }
}
