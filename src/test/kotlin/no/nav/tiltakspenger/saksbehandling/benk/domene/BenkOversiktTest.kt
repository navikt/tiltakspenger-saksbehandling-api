package no.nav.tiltakspenger.saksbehandling.benk.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BenkOversiktTest {

    private fun oversikt(totalAntall: Int) = BenkOversikt<BenkSøknadsbehandling>(
        behandlinger = emptyList(),
        totalAntall = totalAntall,
        totalAntallUfiltrert = 3,
        saksbehandlere = emptyList(),
        besluttere = emptyList(),
    )

    @Test
    fun `avgrensing beholder det ufiltrerte totalet`() {
        oversikt(totalAntall = 0).avgrensTil(emptySet()) shouldBe oversikt(totalAntall = 0)
    }

    @Test
    fun `en paginert oversikt kan ikke avgrenses i minnet`() {
        shouldThrow<IllegalArgumentException> { oversikt(totalAntall = 1).avgrensTil(emptySet()) }
    }
}
