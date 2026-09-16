package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak

/**
 * Oversetter avvisningskoden Tilgangsmaskinen setter i `title` til domenets årsak.
 * Kodene er lest ut av tjenestens `AvvisningsKode` i navikt/populasjonstilgangskontroll (2026-09-16).
 * En kode vi ikke kjenner, blir [TilgangsvurderingAvvistÅrsak.UKJENT]; tilgangen er allerede avgjort av statuskoden, så metadata skal aldri felle kallet.
 */
fun tilTilgangsvurderingAvvistÅrsak(kode: String): TilgangsvurderingAvvistÅrsak = when (kode) {
    "AVVIST_STRENGT_FORTROLIG_ADRESSE" -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG
    "AVVIST_STRENGT_FORTROLIG_UTLAND" -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND
    "AVVIST_FORTROLIG_ADRESSE" -> TilgangsvurderingAvvistÅrsak.FORTROLIG
    "AVVIST_SKJERMING" -> TilgangsvurderingAvvistÅrsak.SKJERMET
    "AVVIST_HABILITET" -> TilgangsvurderingAvvistÅrsak.HABILITET
    "AVVIST_VERGEMÅL" -> TilgangsvurderingAvvistÅrsak.VERGE
    "AVVIST_GEOGRAFISK" -> TilgangsvurderingAvvistÅrsak.GEOGRAFISK
    "AVVIST_UKJENT_BOSTED" -> TilgangsvurderingAvvistÅrsak.UKJENT_BOSTED
    "AVVIST_PERSON_UTLAND" -> TilgangsvurderingAvvistÅrsak.PERSON_UTLAND
    "AVVIST_AVDØD" -> TilgangsvurderingAvvistÅrsak.AVDØD
    else -> TilgangsvurderingAvvistÅrsak.UKJENT
}
