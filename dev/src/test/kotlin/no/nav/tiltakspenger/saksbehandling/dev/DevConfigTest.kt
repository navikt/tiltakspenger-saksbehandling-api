package no.nav.tiltakspenger.saksbehandling.dev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentProfile
import no.nav.tiltakspenger.saksbehandling.infra.setup.ProdConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.miljøkonfigurasjonFor
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DevConfigTest {
    @Test
    fun `dev-gcp velger DevConfig`() {
        miljøkonfigurasjonFor("dev-gcp") shouldBe DevConfig
        DevConfig.environmentProfile shouldBe EnvironmentProfile.ANNEN_NAIS_KLYNGE
        DevConfig.gcpProsjektId shouldBe "tpts-dev-6211"
    }

    @Test
    fun `prod-gcp bruker ProdConfig selv om dev-leverandøren er på classpath`() {
        val konfigurasjon = miljøkonfigurasjonFor("prod-gcp")

        konfigurasjon shouldBe ProdConfig
        konfigurasjon.tilbakekrevingshendelsePolicy shouldBe TilbakekrevingshendelsePolicy.BeholdAlle
        konfigurasjon.gcpProsjektId shouldBe "tpts-prod-b5ff"
        miljøkonfigurasjonFor("prod-gcp") { throw AssertionError("Prod skal ikke lete etter leverandører") } shouldBe ProdConfig
    }

    @Test
    fun `utviklingsimaget kan ikke starte uten klyngenavn fordi lokalmodulen mangler`() {
        shouldThrow<IllegalStateException> { miljøkonfigurasjonFor(null) }.message shouldBe
            "NAIS_CLUSTER_NAME mangler, og lokal kjøring krever nøyaktig én Miljøkonfigurasjonsleverandør for lokal fra lokalmodulen, men fant 0."
    }

    @Test
    fun `V3-simulering har dev-spesifikt endepunkt og Texas-mål`() {
        DevConfig.helvedSimuleringUrl shouldBe "http://simulering.helved"
        DevConfig.helvedSimuleringScope shouldBe "dev-gcp:helved:simulering"
    }

    @Test
    fun `dev har egne valg for saker, utbetalinger og tilbakekreving`() {
        DevConfig.oppdragsversjonForNyeSaker shouldBe Oppdragsversjon.V3
        DevConfig.saksnummerGenerator.generer(LocalDate.of(2021, 1, 1)).verdi shouldBe "202101011001"
        DevConfig.utbetalingerFølgesOppFra shouldBe LocalDateTime.of(2026, 9, 25, 0, 0)
        DevConfig.tilbakekrevingshendelsePolicy shouldBe TilbakekrevingshendelsePolicy(
            testsaksnummerPrefiks = "BF",
            slettHendelserMedUkjentSaksnummer = true,
            slettUleseligeHendelser = true,
            loggDeserialiseringsfeilSomError = false,
        )
    }
}
