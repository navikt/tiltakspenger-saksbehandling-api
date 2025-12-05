package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse.Companion.toBegrunnelse
import org.junit.jupiter.api.Test

class BegrunnelseTest {
    @Test
    fun `null dersom strengen er blank`() {
        "".toBegrunnelse() shouldBe null
    }

    @Test
    fun `null dersom strengen er en space`() {
        " ".toBegrunnelse() shouldBe null
    }

    @Test
    fun `null dersom strengen er newline`() {
        "\n".toBegrunnelse() shouldBe null
    }

    @Test
    fun `begrunnelse dersom strengen har en verdi`() {
        "a".toBegrunnelse() shouldBe Begrunnelse.createOrThrow("a")
    }

    @Test
    fun `toString er stjernet ut`() {
        Begrunnelse.createOrThrow("personinfo").toString() shouldBe "*****"
    }
}
