package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/**
 * Metadata om en avvist tilgangsvurdering fra tilgangsmaskinen.
 *
 * Skal kun brukes til logging (sikkerlogg) og eventuell lagring for notoritet/debugging.
 * Ingen domenelogikk skal baseres på disse feltene – bruk [Tilgangsvurdering.Avvist.årsak] og [Tilgangsvurdering.Avvist.begrunnelse] til det.
 */
data class AvvistMetadata(
    /**
     * Regeltypen (URI-en) tilgangsmaskinen brukte for avvisningen.
     * Den er lik for alle avvisninger; [avvisningskode] sier hvilken regel som avviste.
     * Kun for logging/debugging.
     */
    val type: String,
    /**
     * Avvisningskoden tilgangsmaskinen svarte med, rå slik den står i `title` (f.eks. `AVVIST_SKJERMING`).
     * Kun for logging/debugging.
     */
    val avvisningskode: String,
    /**
     * Nav-identen til saksbehandleren som ble nektet tilgang.
     * Kun for logging/notoritet.
     */
    val navIdent: String,
    /**
     * Ident (fnr) til personen det ble sjekket tilgang mot.
     * PII – kun for sikkerlogg/notoritet, aldri i vanlig logg.
     */
    val brukerIdent: Fnr,
) {
    /** Maskerer [brukerIdent] (PII) slik at den ikke havner tilfeldigvis i logger. */
    override fun toString(): String = "AvvistMetadata(type=$type, avvisningskode=$avvisningskode, navIdent=$navIdent, brukerIdent=*****)"
}
