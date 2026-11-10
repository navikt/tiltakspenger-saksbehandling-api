package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HelvedSimuleringEnvironmentConfigTest {
    @Test
    fun `V3-simulering har miljøspesifikt endepunkt og Texas-mål`() {
        listOf(
            EnvironmentProfile.LOCAL to HelvedSimuleringKonfigurasjon(
                url = "http://host.docker.internal:8091",
                scope = "localhost",
            ),
            EnvironmentProfile.DEV to HelvedSimuleringKonfigurasjon(
                url = "http://simulering.helved",
                scope = "dev-gcp:helved:simulering",
            ),
            EnvironmentProfile.PROD to HelvedSimuleringKonfigurasjon(
                url = "http://simulering.helved",
                scope = "prod-gcp:helved:simulering",
            ),
        ).forEach { (environmentProfile, expected) ->
            helvedSimuleringKonfigurasjonFor(environmentProfile) shouldBe expected
        }
    }
}
