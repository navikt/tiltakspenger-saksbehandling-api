package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import org.junit.jupiter.api.Test

class LoggkontekstTest {
    private val sakId = SakId.random()
    private val behandlingId = RammebehandlingId.random()
    private val saksnummer = Saksnummer("202501011001")

    private fun behandling(
        saksbehandler: String?,
        beslutter: String?,
    ): AttesterbarBehandling {
        val behandling = mockk<AttesterbarBehandling>()
        every { behandling.sakId } returns sakId
        every { behandling.saksnummer } returns saksnummer
        every { behandling.id } returns behandlingId
        every { behandling.saksbehandler } returns saksbehandler
        every { behandling.beslutter } returns beslutter
        return behandling
    }

    @Test
    fun `tar med alle feltene når de finnes`() {
        val correlationId = CorrelationId.generate()

        behandling(saksbehandler = "Z111111", beslutter = "Z222222").loggkontekst(correlationId) shouldBe
            "sakId: $sakId, saksnummer: 202501011001, behandlingId: $behandlingId, saksbehandler: Z111111, beslutter: Z222222, correlationId: $correlationId"
    }

    @Test
    fun `utelater feltene som mangler`() {
        behandling(saksbehandler = null, beslutter = null).loggkontekst() shouldBe
            "sakId: $sakId, saksnummer: 202501011001, behandlingId: $behandlingId"
    }
}
