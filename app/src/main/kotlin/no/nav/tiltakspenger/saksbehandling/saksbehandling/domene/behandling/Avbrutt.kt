package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import java.time.LocalDateTime

data class Avbrutt(
    val tidspunkt: LocalDateTime,
    val saksbehandler: String,
    val begrunnelse: String,
)
