package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

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

/**
 * Utfallet av et bulkoppslag mot tilgangsmaskinen.
 *
 * Typen er skilt fra [Tilgangsvurdering] fordi bulkoppslaget ikke har metadata per person.
 * Bulkoppslaget bruker i tillegg Tilgangsmaskinens komplette regelsett, som kan avvise av flere grunner enn kjernereglene.
 */
sealed interface TilgangsvurderingBulk {
    data object Godkjent : TilgangsvurderingBulk

    data class Avvist(
        val årsak: TilgangsvurderingAvvistÅrsak,
        val begrunnelse: String,
    ) : TilgangsvurderingBulk
}

/**
 * Grunnen Tilgangsmaskinen avviste tilgangen med, i vårt vokabular.
 *
 * STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND, FORTROLIG, SKJERMET og HABILITET kommer fra kjernereglene.
 * GEOGRAFISK, UKJENT_BOSTED, PERSON_UTLAND, AVDØD og VERGE kommer fra de overstyrbare reglene, som bare det komplette regelsettet (bulk) bruker.
 * UKJENT er koden vi ikke kjenner igjen, og lar en ny regel hos Tilgangsmaskinen passere uten å felle kallet.
 * https://confluence.adeo.no/spaces/TM/pages/621546888/Tilgangsmaskin+API+og+regelsett
 */
enum class TilgangsvurderingAvvistÅrsak {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    SKJERMET,
    HABILITET,
    VERGE,
    GEOGRAFISK,
    UKJENT_BOSTED,
    PERSON_UTLAND,
    AVDØD,

    /**
     * En avvisningskode vi ikke kjenner.
     * Tilgangen er avvist, men vi vet ikke hvorfor, og raden får ingen markør.
     */
    UKJENT,
    ;

    /**
     * Enkeltoppslaget mot `/api/v1/kjerne` gir i praksis bare de fem første årsakene.
     * En avvisning skal likevel bli en 403 uansett hvilken regel som avviste, så de øvrige faller til [Tilgangsnektårsak.ANNET].
     * VERGE beholder mappingen fordi [Tilgangsnektårsak] har en verdi for det, selv om kjernereglene i dag ikke avviser på vergemål.
     */
    fun toTilgangsnektårsak(): Tilgangsnektårsak {
        return when (this) {
            STRENGT_FORTROLIG -> Tilgangsnektårsak.KODE_6
            STRENGT_FORTROLIG_UTLAND -> Tilgangsnektårsak.KODE_6_UTLAND
            FORTROLIG -> Tilgangsnektårsak.KODE_7
            SKJERMET -> Tilgangsnektårsak.SKJERMET
            HABILITET -> Tilgangsnektårsak.HABILITET
            VERGE -> Tilgangsnektårsak.VERGE
            GEOGRAFISK, UKJENT_BOSTED, PERSON_UTLAND, AVDØD, UKJENT -> Tilgangsnektårsak.ANNET
        }
    }
}
