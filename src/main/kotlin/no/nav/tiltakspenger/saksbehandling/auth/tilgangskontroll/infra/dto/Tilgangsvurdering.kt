package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import no.nav.tiltakspenger.saksbehandling.felles.exceptions.Tilgangsnektårsak

sealed interface Tilgangsvurdering {
    data object Godkjent : Tilgangsvurdering

    /**
     * En avvist tilgangsvurdering fra tilgangsmaskinen.
     *
     * [årsak] og [begrunnelse] er de eneste feltene som brukes i domenelogikken (mapping til tilgangsnektårsak og feilmelding til saksbehandler).
     * Øvrig kontekst ligger i [metadata] og skal kun brukes til logging/notoritet.
     */
    data class Avvist(
        /**
         * Kategorisert årsak til avvisningen.
         * Styrer hvilken [no.nav.tiltakspenger.saksbehandling.felles.exceptions.Tilgangsnektårsak] saksbehandleren får.
         */
        val årsak: TilgangsvurderingAvvistÅrsak,
        /**
         * Menneskelesbar begrunnelse fra tilgangsmaskinen.
         * Vises til saksbehandleren i feilmeldingen.
         */
        val begrunnelse: String,
        val metadata: AvvistMetadata,
    ) : Tilgangsvurdering
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
