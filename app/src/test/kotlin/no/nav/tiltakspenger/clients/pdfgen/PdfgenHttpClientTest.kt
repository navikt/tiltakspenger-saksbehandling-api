package no.nav.tiltakspenger.clients.pdfgen

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.clients.pdfgen.PdfgenHttpClient
import org.junit.jupiter.api.Test

class PdfgenHttpClientTest {

    @Test
    fun genererMeldekortPdf() {
        runTest {
            val utbetalingsvedtak = ObjectMother.utbetalingsvedtak()
            PdfgenHttpClient("unused").genererUtbetalingsvedtak(
                utbetalingsvedtak,
                tiltaksdeltagelser = listOf(ObjectMother.tiltaksdeltagelse()),
            ) { ObjectMother.saksbehandler().brukernavn }
        }
    }
}
