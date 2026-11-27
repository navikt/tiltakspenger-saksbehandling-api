package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

class MiljøkonfigurasjonTest {
    private val annenKonfigurasjon = object : EnvironmentConfig by LocalConfig {
        override val environmentProfile = EnvironmentProfile.ANNEN_NAIS_KLYNGE
    }

    @Test
    fun `uten klynge brukes lokal konfigurasjon uten oppslag av leverandører`() {
        miljøkonfigurasjonFor(null) { throw AssertionError("Leverandørene skal ikke hentes") } shouldBeSameInstanceAs LocalConfig
    }

    @Test
    fun `med klynge lokal brukes lokal konfigurasjon uten oppslag av leverandører`() {
        miljøkonfigurasjonFor("lokal") { throw AssertionError("Leverandørene skal ikke hentes") } shouldBeSameInstanceAs LocalConfig
    }

    // Valget av ProdConfig testes i DevConfigTest, som har miljøvariablene ProdConfig leser.

    @Test
    fun `en annen klynge får konfigurasjonen fra leverandøren for klyngen`() {
        val leverandører = listOf(
            leverandør("en-annen-klynge") { throw AssertionError("Feil leverandør") },
            leverandør("test-klynge") { annenKonfigurasjon },
        )

        miljøkonfigurasjonFor("test-klynge") { leverandører } shouldBeSameInstanceAs annenKonfigurasjon
    }

    @Test
    fun `en annen klynge krever nøyaktig én leverandør`() {
        val treff = leverandør("test-klynge") { annenKonfigurasjon }
        listOf(
            emptyList<Miljøkonfigurasjonsleverandør>() to 0,
            listOf(leverandør("en-annen-klynge") { annenKonfigurasjon }) to 0,
            listOf(treff, treff) to 2,
        ).forEach { (leverandører, antall) ->
            shouldThrow<IllegalStateException> {
                miljøkonfigurasjonFor("test-klynge") { leverandører }
            }.message shouldBe
                "NAIS_CLUSTER_NAME er test-klynge, og det krever nøyaktig én Miljøkonfigurasjonsleverandør for klyngen, men fant $antall."
        }
    }

    @Test
    fun `leverandøren kan ikke gi lokal profil`() {
        shouldThrow<IllegalStateException> {
            miljøkonfigurasjonFor("test-klynge") { listOf(leverandør("test-klynge") { LocalConfig }) }
        }.message shouldBe "Konfigurasjonen for klyngen test-klynge må ha profilen ANNEN_NAIS_KLYNGE, men har LOCAL."
    }

    @Test
    fun `leverandøren kan ikke gi prod-profil`() {
        val medProdProfil = object : EnvironmentConfig by LocalConfig {
            override val environmentProfile = EnvironmentProfile.PROD
        }

        shouldThrow<IllegalStateException> {
            miljøkonfigurasjonFor("test-klynge") { listOf(leverandør("test-klynge") { medProdProfil }) }
        }.message shouldBe "Konfigurasjonen for klyngen test-klynge må ha profilen ANNEN_NAIS_KLYNGE, men har PROD."
    }

    private fun leverandør(
        klynge: String,
        konfigurasjon: () -> EnvironmentConfig,
    ) = object : Miljøkonfigurasjonsleverandør {
        override val klynge = klynge

        override fun hentKonfigurasjon() = konfigurasjon()
    }
}
