package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

/**
 * Wire-DTO for én person i request-body mot tilgangsmaskinens bulk-endepunkt.
 *
 * Lever kun i infra og serialiseres til JSON. [brukerId] er et fnr (PII).
 */
data class TilgangPersonRequestDto(
    val brukerId: String,
) {
    /** Maskerer [brukerId] (fnr, PII) slik at den ikke havner tilfeldigvis i logger. */
    override fun toString(): String = "TilgangPersonRequestDto(brukerId=*****)"
}
