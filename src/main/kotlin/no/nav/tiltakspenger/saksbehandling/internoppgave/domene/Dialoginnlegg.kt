package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

/**
 * Ett innlegg i den løpende dialogen om en oppgave.
 * Alle saksbehandlere kan skrive innlegg, også de som ikke er tildelt oppgaven.
 */
data class Dialoginnlegg(
    val saksbehandler: String,
    val tidspunkt: LocalDateTime,
    val tekst: NonBlankString,
) {
    init {
        require(saksbehandler.isNotBlank()) { "Saksbehandler kan ikke være blank" }
    }

    override fun toString(): String = "Dialoginnlegg(saksbehandler=$saksbehandler, tidspunkt=$tidspunkt, tekst=*****)"
}
