package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MiljøkonfigurasjonTest {
    private val lokalKonfigurasjon = object : EnvironmentConfig by Configuration {}

    private val annenKonfigurasjon = object : EnvironmentConfig by Configuration {
        override val environmentProfile = EnvironmentProfile.ANNEN_NAIS_KLYNGE
    }

    @Test
    fun `uten klynge brukes leverandøren for lokal klynge`() {
        miljøkonfigurasjonFor(null) { listOf(leverandør(LOKAL_KLYNGE) { lokalKonfigurasjon }) } shouldBeSameInstanceAs lokalKonfigurasjon
    }

    @Test
    fun `uten klynge og uten lokal leverandør feiler oppstarten`() {
        shouldThrow<IllegalStateException> {
            miljøkonfigurasjonFor(null) { emptyList() }
        }.message shouldBe "NAIS_CLUSTER_NAME mangler, og lokal kjøring krever nøyaktig én Miljøkonfigurasjonsleverandør for lokal fra lokalmodulen, men fant 0."
    }

    @Test
    fun `lokal klynge krever lokal profil`() {
        shouldThrow<IllegalStateException> {
            miljøkonfigurasjonFor(LOKAL_KLYNGE) { listOf(leverandør(LOKAL_KLYNGE) { annenKonfigurasjon }) }
        }.message shouldBe "Konfigurasjonen for klyngen lokal må ha profilen LOCAL, men har ANNEN_NAIS_KLYNGE."
    }

    @Test
    fun `testene kjører med den lokale konfigurasjonen fra lokalmodulen`() {
        Configuration.environmentProfile shouldBe EnvironmentProfile.LOCAL
        Configuration.tilbakekrevingshendelsePolicy shouldBe TilbakekrevingshendelsePolicy.BeholdAlle
        Configuration.saksnummerGenerator.generer(LocalDate.of(2021, 1, 1)).verdi shouldBe "202101011001"
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
            miljøkonfigurasjonFor("test-klynge") { listOf(leverandør("test-klynge") { lokalKonfigurasjon }) }
        }.message shouldBe "Konfigurasjonen for klyngen test-klynge må ha profilen ANNEN_NAIS_KLYNGE, men har LOCAL."
    }

    @Test
    fun `leverandøren kan ikke gi prod-profil`() {
        val medProdProfil = object : EnvironmentConfig by Configuration {
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
