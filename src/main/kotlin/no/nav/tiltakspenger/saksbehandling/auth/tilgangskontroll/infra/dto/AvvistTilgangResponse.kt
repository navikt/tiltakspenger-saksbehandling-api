package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

data class AvvistTilgangResponse(
    val type: String,
    val title: String,
    val status: Int,
    val brukerIdent: String,
    val navIdent: String,
    val begrunnelse: String,
) {
    /** Maskerer [brukerIdent] (fnr, PII) slik at den ikke havner tilfeldigvis i logger. */
    override fun toString(): String =
        "AvvistTilgangResponse(type=$type, title=$title, status=$status, brukerIdent=*****, navIdent=$navIdent, begrunnelse=$begrunnelse)"

    fun tilAvvistTilgangsvurdering(): Tilgangsvurdering.Avvist {
        val årsak = when (title) {
            "AVVIST_STRENGT_FORTROLIG_ADRESSE" -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG

            "AVVIST_STRENGT_FORTROLIG_UTLAND" -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND

            "AVVIST_FORTROLIG_ADRESSE" -> TilgangsvurderingAvvistÅrsak.FORTROLIG

            "AVVIST_SKJERMING" -> TilgangsvurderingAvvistÅrsak.SKJERMET

            "AVVIST_HABILITET" -> TilgangsvurderingAvvistÅrsak.HABILITET

            "AVVIST_VERGE" -> TilgangsvurderingAvvistÅrsak.VERGE

            "AVVIST_MANGLENDE_DATA" -> {
                throw IllegalStateException("Kan ikke avgjøre om tilgang er godkjent på grunn av manglende informasjon fra baksystemer ")
            }

            else -> {
                throw IllegalArgumentException("Ukjent avvisningstype fra tilgangsmaskinen: $type")
            }
        }
        return Tilgangsvurdering.Avvist(
            årsak = årsak,
            begrunnelse = begrunnelse,
            metadata = AvvistMetadata(
                type = type,
                navIdent = navIdent,
                brukerIdent = brukerIdent,
            ),
        )
    }
}
