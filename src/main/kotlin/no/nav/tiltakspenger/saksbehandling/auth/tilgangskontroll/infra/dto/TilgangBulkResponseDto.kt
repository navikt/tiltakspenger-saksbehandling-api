package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Wire-DTO for 207-svaret fra tilgangsmaskinens bulk-endepunkt (`/api/v1/bulk/obo`).
 *
 * Lever kun i infra: klienten deserialiserer hit og mapper til domenevennlig [tilTilgangPerFnr] før noe krysser porten.
 * Domenet skal aldri se HTTP-statuser herfra.
 */
data class TilgangBulkResponseDto(
    val resultater: List<TilgangResponse>,
) {
    data class TilgangResponse(
        val brukerId: String,
        val status: Int,
    ) {
        /** Maskerer [brukerId] (fnr, PII) slik at den ikke havner tilfeldigvis i logger. */
        override fun toString(): String = "TilgangResponse(brukerId=*****, status=$status)"
    }

    fun tilTilgangPerFnr(): Map<Fnr, Boolean> =
        resultater.associate { Fnr.fromString(it.brukerId) to (it.status == HTTP_STATUS_TILGANG) }

    private companion object {
        const val HTTP_STATUS_TILGANG = 204
    }
}
