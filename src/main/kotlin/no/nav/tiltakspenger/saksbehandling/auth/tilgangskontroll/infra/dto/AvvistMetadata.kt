package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

/**
 * Metadata om en avvist tilgangsvurdering fra tilgangsmaskinen.
 *
 * Skal kun brukes til logging (sikkerlogg) og eventuell lagring for notoritet/debugging.
 * Ingen domenelogikk skal baseres på disse feltene – bruk [Tilgangsvurdering.Avvist.årsak] og [Tilgangsvurdering.Avvist.begrunnelse] til det.
 */
data class AvvistMetadata(
    /**
     * Regeltypen/URI-en tilgangsmaskinen brukte for avvisningen (f.eks. `AVVIST_SKJERMING`).
     * Kun for logging/debugging.
     */
    val type: String,
    /**
     * Nav-identen til saksbehandleren som ble nektet tilgang.
     * Kun for logging/notoritet.
     */
    val navIdent: String,
    /**
     * Ident (fnr) til personen det ble sjekket tilgang mot.
     * PII – kun for sikkerlogg/notoritet, aldri i vanlig logg.
     */
    val brukerIdent: String,
) {
    /** Maskerer [brukerIdent] (PII) slik at den ikke havner tilfeldigvis i logger. */
    override fun toString(): String = "AvvistMetadata(type=$type, navIdent=$navIdent, brukerIdent=*****)"
}
