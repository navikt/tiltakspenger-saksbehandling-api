package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

/**
 * @param originalResponseBody Skal som regel være JSON, men det er ikke en garanti.
 */
data class SimuleringMedMetadata(
    val simulering: Simulering,
    val originalResponseBody: String,
)
