package no.nav.tiltakspenger.saksbehandling.felles

import java.time.LocalDateTime

data class SattPåVentBegrunnelse(
    val tidspunkt: LocalDateTime,
    val sattPåVentAv: String,
    val begrunnelse: String,
)
