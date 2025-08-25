package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

data class PersonRequestItem(
    val brukerId: String,
    val type: String = "KJERNE_REGELTYPE",
)
