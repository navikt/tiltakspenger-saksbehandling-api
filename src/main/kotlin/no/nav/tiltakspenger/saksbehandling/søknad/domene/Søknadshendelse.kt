package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

/**
 * Én hendelse i historikken over avbrytelser og gjenopprettinger av en søknad.
 * Historikken er append-only.
 * [Søknad.avbrutt] er gjeldende tilstand, mens hendelsene forteller hvordan søknaden kom dit.
 */
sealed interface Søknadshendelse {
    val tidspunkt: LocalDateTime

    /** NavIdent til saksbehandleren som utførte handlingen. */
    val utførtAv: String

    val begrunnelse: NonBlankString?

    data class Avbrutt(
        override val tidspunkt: LocalDateTime,
        override val utførtAv: String,
        override val begrunnelse: NonBlankString,
    ) : Søknadshendelse

    /**
     * Søknaden ble tatt opp igjen etter å ha vært avbrutt.
     * Begrunnelsen er valgfri fordi gjenopprettingen som regel forklares av at det opprettes en ny behandling.
     */
    data class Gjenopprettet(
        override val tidspunkt: LocalDateTime,
        override val utførtAv: String,
        override val begrunnelse: NonBlankString?,
    ) : Søknadshendelse
}
