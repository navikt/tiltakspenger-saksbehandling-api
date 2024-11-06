package no.nav.tiltakspenger.vedtak.clients.pdfgen

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class PdfgenHttpClientTest {

    @Test
    fun genererMeldekortPdf() {
        runTest {
            val utbetalingsvedtak = ObjectMother.utbetalingsvedtak()
            PdfgenHttpClient("unused").genererMeldekortPdf(utbetalingsvedtak) { ObjectMother.saksbehandler().brukernavn }
        }
    }
}
