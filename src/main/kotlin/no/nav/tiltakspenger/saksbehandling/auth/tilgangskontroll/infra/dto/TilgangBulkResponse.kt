package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

data class TilgangBulkResponse(
    val ansattId: String,
    val resultater: List<TilgangResponse>,
) {
    data class TilgangResponse(
        val brukerId: String,
        val status: Int,
        val detaljer: AvvistTilgangResponse? = null,
    ) {
        fun harTilgangTilPerson() = status == 204
    }
}
