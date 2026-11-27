package no.nav.tiltakspenger.saksbehandling.dev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentProfile
import org.junit.jupiter.api.Test

class DevAppTest {
    /**
     * `Configuration` initialiseres én gang per JVM.
     * Testoppgaven setter derfor `NAIS_CLUSTER_NAME` til dev-gcp og bruker en ny JVM for hver testklasse.
     */
    @Test
    fun `Configuration finner DevConfig gjennom ServiceLoader i dev`() {
        var appenErStartet = false

        startDevApp("dev-gcp") {
            Configuration.environmentProfile shouldBe EnvironmentProfile.ANNEN_NAIS_KLYNGE
            Configuration.isNais() shouldBe true
            Configuration.isProd() shouldBe false
            Configuration.tilbakekrevingshendelsePolicy shouldBe DevConfig.tilbakekrevingshendelsePolicy
            appenErStartet = true
        }

        appenErStartet shouldBe true
    }

    @Test
    fun `utviklingsimaget avviser prod og andre klynger før appen starter`() {
        listOf("prod-gcp", "dev-fss", "", null).forEach { klynge ->
            shouldThrow<IllegalStateException> {
                startDevApp(klynge) { throw AssertionError("Appen skal ikke starte") }
            }.message shouldBe "Utviklingsimaget kan bare starte i dev-gcp, men NAIS_CLUSTER_NAME er $klynge."
        }
    }
}
