package no.nav.tiltakspenger.saksbehandling.routes.søknad

import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.hentEllerOpprettSak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.mottaSøknad
import no.nav.tiltakspenger.saksbehandling.routes.routes
import org.junit.jupiter.api.Test

internal class MottaSøknadTest {
    @Test
    fun `kan motta søknad`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val fnr = Fnr.random()
                val saksnummer = hentEllerOpprettSak(tac, fnr)
                mottaSøknad(
                    tac = tac,
                    fnr = fnr,
                    saksnummer = saksnummer,
                )
            }
        }
    }
}
