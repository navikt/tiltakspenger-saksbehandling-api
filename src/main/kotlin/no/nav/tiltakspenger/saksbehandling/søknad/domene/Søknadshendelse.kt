package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

/**
 * Én hendelse i historikken over avbrytelser og gjenåpninger av en søknad.
 * Historikken er append-only.
 */
sealed interface Søknadshendelse {
    val tidspunkt: LocalDateTime

    val utførtAv: String

    val begrunnelse: NonBlankString?

    data class Avbrutt(
        override val tidspunkt: LocalDateTime,
        override val utførtAv: String,
        override val begrunnelse: NonBlankString,
    ) : Søknadshendelse

    data class Gjenåpnet(
        override val tidspunkt: LocalDateTime,
        override val utførtAv: String,
        override val begrunnelse: NonBlankString?,
    ) : Søknadshendelse
}
