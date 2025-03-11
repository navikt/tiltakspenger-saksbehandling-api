package no.nav.tiltakspenger.saksbehandling.clients.pdfgen

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.clients.pdfgen.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
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
