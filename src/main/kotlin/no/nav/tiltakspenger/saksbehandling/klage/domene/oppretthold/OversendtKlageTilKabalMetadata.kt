package no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold

import java.time.LocalDateTime

data class OversendtKlageTilKabalMetadata(
    val request: String,
    val response: String,
    val oversendtTidspunkt: LocalDateTime,
)
