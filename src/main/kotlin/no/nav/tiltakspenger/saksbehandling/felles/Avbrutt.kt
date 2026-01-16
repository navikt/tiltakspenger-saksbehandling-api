package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

data class Avbrutt(
    val tidspunkt: LocalDateTime,
    val saksbehandler: String,
    val begrunnelse: NonBlankString,
)
