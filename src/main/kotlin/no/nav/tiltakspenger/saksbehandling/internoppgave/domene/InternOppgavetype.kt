package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

enum class InternOppgavetype {
    ENDRET_TILTAKSDELTAKELSE,
    ;

    val tillatteRevurderingstyper: Set<InternOppgaveløsning.Revurderingstype>
        get() = when (this) {
            ENDRET_TILTAKSDELTAKELSE -> setOf(
                InternOppgaveløsning.Revurderingstype.STANS,
                InternOppgaveløsning.Revurderingstype.FORLENGELSE,
                InternOppgaveløsning.Revurderingstype.OMGJØRING,
            )
        }
}
