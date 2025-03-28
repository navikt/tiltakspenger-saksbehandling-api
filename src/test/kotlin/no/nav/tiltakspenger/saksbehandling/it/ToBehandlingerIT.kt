package no.nav.tiltakspenger.saksbehandling.it

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksett
import no.nav.tiltakspenger.saksbehandling.routes.routes
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
                        routing { routes(tac) }
                    }
                    val fnr = Fnr.random()
                    val førsteVirkningsperiode = Periode(1.mars(2024), 15.mars(2024))
                    val andreVirkningsperiode = Periode(16.april(2024), 21.april(2024))
                    val (sak) = this.iverksett(tac, fnr, førsteVirkningsperiode)

                    sak.let {
                        it.soknader.size shouldBe 1
                        it.behandlinger.size shouldBe 1
                        it.meldeperiodeKjeder.size shouldBe 2
                    }

                    val (sakEtterAndreFørstegangsbehandling) = this.iverksett(tac, fnr, andreVirkningsperiode)

                    sakEtterAndreFørstegangsbehandling.let {
                        it.soknader.size shouldBe 2
                        it.behandlinger.size shouldBe 2
                        it.meldeperiodeKjeder.size shouldBe 3
                        it.meldeperiodeKjeder[2].periode shouldBe Periode(8.april(2024), 21.april(2024))
                    }
                }
            }
        }
    }
}
