package no.nav.tiltakspenger.saksbehandling.clients.pdfgen

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.dokument.infra.toJsonRequest
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class UtbetalingsvedtakDTOTest {

    @Test
    fun `kan serialiseres`() = runTest {
        val utbetalingsvedtak = ObjectMother.utbetalingsvedtak()
        val tiltaksdeltagelser = listOf(ObjectMother.tiltaksdeltagelse())

        utbetalingsvedtak.toJsonRequest(
            hentSaksbehandlersNavn = { "Saksbehandler Navn" },
            tiltaksdeltagelser = tiltaksdeltagelser,
        )
    }
}
