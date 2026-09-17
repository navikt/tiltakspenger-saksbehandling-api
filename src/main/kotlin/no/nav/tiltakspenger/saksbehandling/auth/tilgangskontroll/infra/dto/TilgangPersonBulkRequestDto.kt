package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Wire-DTO for request-body mot tilgangsmaskinens bulk-endepunkt.
 *
 * Lever kun i infra og serialiseres til JSON.
 * Delegeringen til [List] gjør at json-en blir et rent array av fnr, ikke et objekt.
 * [brukerIder] er fnr (PII).
 */
data class TilgangPersonBulkRequestDto private constructor(
    private val brukerIder: List<String>,
) : List<String> by brukerIder {
    /** Maskerer [brukerIder] (fnr, PII) slik at den ikke havner tilfeldigvis i logger. */
    override fun toString(): String = "TilgangPersonBulkRequestDto(brukerId=*****)"

    companion object {
        fun fraFnrs(fnrs: List<Fnr>): TilgangPersonBulkRequestDto = TilgangPersonBulkRequestDto(brukerIder = fnrs.map { it.verdi })
    }
}
