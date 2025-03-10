package no.nav.tiltakspenger.vedtak.felles.journalføring

import no.nav.tiltakspenger.vedtak.felles.PdfA
import java.util.Base64

data class PdfOgJson(
    val pdf: PdfA,
    val json: String,
) {
    fun pdfAsBase64(): String = pdf.toBase64()
    fun jsonAsBase64(): String = Base64.getEncoder().encodeToString(json.toByteArray())
}
