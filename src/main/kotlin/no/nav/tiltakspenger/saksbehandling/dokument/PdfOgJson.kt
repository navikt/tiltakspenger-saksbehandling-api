package no.nav.tiltakspenger.saksbehandling.dokument

import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import java.util.Base64

data class PdfOgJson(
    val pdf: PdfA,
    val json: String,
) {
    fun pdfAsBase64(): String = pdf.toBase64()
    fun jsonAsBase64(): String = Base64.getEncoder().encodeToString(json.toByteArray())
}
