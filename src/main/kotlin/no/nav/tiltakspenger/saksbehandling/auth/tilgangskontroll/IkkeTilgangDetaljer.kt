package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

sealed interface IkkeTilgangDetaljer {
    data class AvvistTilgang(
        val regel: String,
        val begrunnelse: String,
    ) : IkkeTilgangDetaljer
    data class UkjentFeil(val feilmelding: String?) : IkkeTilgangDetaljer
}
