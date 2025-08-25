package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

data class AvvistTilgangResponse(
    val type: String,
    val title: String,
    val status: Int,
    val brukerIdent: String,
    val navIdent: String,
    val begrunnelse: String,
)
