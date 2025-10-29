package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import no.nav.tiltakspenger.saksbehandling.felles.exceptions.Tilgangsnektårsak

sealed interface Tilgangsvurdering {
    data object Godkjent : Tilgangsvurdering
    data class Avvist(
        val type: String,
        val årsak: TilgangsvurderingAvvistÅrsak,
        val status: Int,
        val brukerIdent: String,
        val navIdent: String,
        val begrunnelse: String,
    ) : Tilgangsvurdering

    data object GenerellFeilMotTilgangsmaskin : Tilgangsvurdering
}

/*
* https://confluence.adeo.no/spaces/TM/pages/621546888/Tilgangsmaskin+API+og+regelsett
*/
enum class TilgangsvurderingAvvistÅrsak {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    SKJERMET,
    HABILITET,
    VERGE,
    ;

    fun toTilgangsnektårsak(): Tilgangsnektårsak = when (this) {
        STRENGT_FORTROLIG -> Tilgangsnektårsak.KODE_6
        STRENGT_FORTROLIG_UTLAND -> Tilgangsnektårsak.KODE_6_UTLAND
        FORTROLIG -> Tilgangsnektårsak.KODE_7
        SKJERMET -> Tilgangsnektårsak.SKJERMET
        HABILITET -> Tilgangsnektårsak.HABILITET
        VERGE -> Tilgangsnektårsak.VERGE
    }
}
