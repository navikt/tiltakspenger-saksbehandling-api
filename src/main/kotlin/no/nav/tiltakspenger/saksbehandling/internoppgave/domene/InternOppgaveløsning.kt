package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import java.time.LocalDateTime

/**
 * Registrerer utfallet av oppgaven uten å opprette en behandling.
 * @param begrunnelse Hvorfor saksbehandler valgte utfallet.
 * @param løst Tidspunktet oppgaven ble løst, som alltid er lik oppgavens sistEndret.
 */
data class InternOppgaveløsning(
    val utfall: Utfall,
    val begrunnelse: Løsningsbegrunnelse,
    val løst: LocalDateTime,
) {
    sealed interface Utfall {
        data object Forkastet : Utfall

        data class Revurdering(
            val type: Revurderingstype,
            val behandlingId: RammebehandlingId,
        ) : Utfall
    }

    enum class Revurderingstype {
        STANS,
        FORLENGELSE,
        OMGJØRING,
    }
}
