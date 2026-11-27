package no.nav.tiltakspenger.saksbehandling.dev

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentProfile
import no.nav.tiltakspenger.saksbehandling.infra.setup.LocalConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.ProdConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.miljøkonfigurasjonFor
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import org.junit.jupiter.api.Test
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
    fun `lokal kjøring bruker LocalConfig selv om dev-leverandøren er på classpath`() {
        val konfigurasjon = miljøkonfigurasjonFor(null)

        konfigurasjon shouldBe LocalConfig
        konfigurasjon.tilbakekrevingshendelsePolicy shouldBe TilbakekrevingshendelsePolicy.BeholdAlle
    }

    @Test
    fun `V3-simulering har dev-spesifikt endepunkt og Texas-mål`() {
        DevConfig.helvedSimuleringUrl shouldBe "http://simulering.helved"
        DevConfig.helvedSimuleringScope shouldBe "dev-gcp:helved:simulering"
    }

    @Test
    fun `dev har egne valg for saker, utbetalinger og tilbakekreving`() {
        DevConfig.oppdragsversjonForNyeSaker shouldBe Oppdragsversjon.V3
        DevConfig.saksnummerGenerator shouldBe SaksnummerGenerator.Dev
        DevConfig.utbetalingerFølgesOppFra shouldBe LocalDateTime.of(2026, 9, 25, 0, 0)
        DevConfig.tilbakekrevingshendelsePolicy shouldBe TilbakekrevingshendelsePolicy(
            testsaksnummerPrefiks = "BF",
            slettHendelserMedUkjentSaksnummer = true,
            slettUleseligeHendelser = true,
            loggDeserialiseringsfeilSomError = false,
        )
    }
}
