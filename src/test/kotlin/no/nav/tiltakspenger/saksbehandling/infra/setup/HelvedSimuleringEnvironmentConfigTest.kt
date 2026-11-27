package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Dev-verdiene forankres i `DevConfigTest` i modulen `dev`. */
class HelvedSimuleringEnvironmentConfigTest {
    @Test
    fun `V3-simulering har miljøspesifikt endepunkt og Texas-mål`() {
        HelvedSimuleringKonfigurasjon.Prod shouldBe HelvedSimuleringKonfigurasjon(
            url = "http://simulering.helved",
            scope = "prod-gcp:helved:simulering",
        )
    }
}
