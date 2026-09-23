package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId

sealed interface InternOppgaveGrunnlag {
    val type: InternOppgavetype
    val nøkkel: String

    data class EndretTiltaksdeltakelse(
        val tiltaksdeltakerId: TiltaksdeltakerId,
        val hendelseId: TiltaksdeltakerHendelseId,
        val beskrivelse: String,
    ) : InternOppgaveGrunnlag {
        override val type: InternOppgavetype = InternOppgavetype.ENDRET_TILTAKSDELTAKELSE
        override val nøkkel: String = tiltaksdeltakerId.toString()

        init {
            require(beskrivelse.isNotBlank()) { "Beskrivelsen kan ikke være blank" }
        }

        override fun toString(): String =
            "EndretTiltaksdeltakelse(tiltaksdeltakerId=$tiltaksdeltakerId, hendelseId=$hendelseId, beskrivelse=*****)"
    }
}
