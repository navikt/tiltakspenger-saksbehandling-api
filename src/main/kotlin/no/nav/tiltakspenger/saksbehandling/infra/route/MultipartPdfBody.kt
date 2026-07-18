package no.nav.tiltakspenger.saksbehandling.infra.route

import java.io.ByteArrayOutputStream

/*
    TODO - pdfgenrs: fjern når det er verifisert at PDF fra pdfgenrs er ok og vi ikke lenger sender to PDF-er ved forhåndsvisning.
 */
internal fun buildMultipartBody(vararg pdfs: ByteArray): ByteArray {
    val boundary = "pdf-boundary"
    return ByteArrayOutputStream().apply {
        pdfs.forEachIndexed { index, pdf ->
            write("--$boundary\r\n".toByteArray())
            write("Content-Type: application/pdf\r\n".toByteArray())
            write("Content-Disposition: attachment; filename=\"brev-${index + 1}.pdf\"\r\n".toByteArray())
            write("\r\n".toByteArray())
            write(pdf)
            write("\r\n".toByteArray())
        }
        write("--$boundary--\r\n".toByteArray())
    }.toByteArray()
}
