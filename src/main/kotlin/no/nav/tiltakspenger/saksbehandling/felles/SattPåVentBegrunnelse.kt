package no.nav.tiltakspenger.saksbehandling.felles

import java.time.LocalDateTime

data class SattPåVentBegrunnelse(
    val tidspunkt: LocalDateTime,
    val saksbehandler: String,
    val begrunnelse: String,
)
