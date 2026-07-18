package no.nav.tiltakspenger.saksbehandling.dokument

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError

/**
 * Feil ved generering av PDF hos pdfgen/pdfgenrs.
 * Bærer den underliggende [HttpKlientError]-en, som kun er ment for feillogging i kallende service/jobb via [no.nav.tiltakspenger.libs.httpklient.loggFeil].
 * Hvilken backend som feilet (pdfgen eller pdfgenrs) framgår av URI-en i feilens metadata.
 */
class KunneIkkeGenererePdf(val feil: HttpKlientError) {
    /**
     * PII-fri: brev-payloaden i metadataen inneholder fnr og navn og skal kun til sikkerlogg via loggFeil.
     */
    override fun toString() = "KunneIkkeGenererePdf(feil=${feil::class.simpleName}, statusCode=${feil.metadata.statusCode})"
}
