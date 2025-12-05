package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev.Companion.toFritekstTilVedtaksbrev
import org.junit.jupiter.api.Test

class FritekstTilVedtaksbrevTest {
    @Test
    fun `null dersom strengen er blank`() {
        "".toFritekstTilVedtaksbrev() shouldBe null
    }

    @Test
    fun `null dersom strengen er en space`() {
        " ".toFritekstTilVedtaksbrev() shouldBe null
    }

    @Test
    fun `null dersom strengen er newline`() {
        "\n".toFritekstTilVedtaksbrev() shouldBe null
    }

    @Test
    fun `FritekstTilVedtaksbrev dersom strengen har en verdi`() {
        "a".toFritekstTilVedtaksbrev() shouldBe FritekstTilVedtaksbrev.createOrThrow("a")
    }

    @Test
    fun `toString er stjernet ut`() {
        FritekstTilVedtaksbrev.createOrThrow("personinfo").toString() shouldBe "*****"
    }
}
