package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test

class ToBehandlingerIT {

    @Test
    fun `kan iverksette 2 behandlinger`() {
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                testApplication {
                    application {
                        jacksonSerialization()
                        configureExceptions()
                        setupAuthentication(texasClient)
                        routing { routes(tac) }
                    }
                    val fnr = Fnr.random()
                    val førsteVirkningsperiode = Periode(1.mars(2024), 15.mars(2024))
                    val andreVirkningsperiode = Periode(16.april(2024), 21.april(2024))
                    val (sak) = this.iverksettSøknadsbehandling(
                        tac,
                        fnr = fnr,
                        virkningsperiode = førsteVirkningsperiode,
                    )

                    sak.let {
                        it.søknader.size shouldBe 1
                        it.rammebehandlinger.size shouldBe 1
                        it.meldeperiodeKjeder.size shouldBe 2
                    }

                    val (sakEtterAndreSøknadsbehandling) = this.iverksettSøknadsbehandling(
                        tac,
                        fnr = fnr,
                        virkningsperiode = andreVirkningsperiode,
                    )

                    sakEtterAndreSøknadsbehandling.let {
                        it.søknader.size shouldBe 2
                        it.rammebehandlinger.size shouldBe 2
                        it.meldeperiodeKjeder.size shouldBe 3
                        it.meldeperiodeKjeder[2].periode shouldBe Periode(8.april(2024), 21.april(2024))
                    }
                }
            }
        }
    }
}
