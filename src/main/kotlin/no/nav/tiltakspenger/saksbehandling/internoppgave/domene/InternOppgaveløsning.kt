package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId

/** Registrerer utfallet av oppgaven uten å opprette en behandling. */
sealed interface InternOppgaveløsning {
    data object Forkastet : InternOppgaveløsning

    data class Revurdering(
        val type: Revurderingstype,
        val behandlingId: RammebehandlingId,
    ) : InternOppgaveløsning

    enum class Revurderingstype {
        STANS,
        FORLENGELSE,
        OMGJØRING,
    }
}
