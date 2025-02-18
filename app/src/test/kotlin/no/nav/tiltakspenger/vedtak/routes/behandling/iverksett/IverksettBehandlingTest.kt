package no.nav.tiltakspenger.vedtak.routes.behandling.iverksett

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.iverksett
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

class IverksettBehandlingTest {
    @Test
    fun `send til beslutter endrer status på behandlingen`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = this.iverksett(tac)
                behandling.innvilgelsesperiode.shouldNotBeNull()
                behandling.status shouldBe Behandlingsstatus.VEDTATT
                behandling.erNyFlyt shouldBe true
                behandling.saksopplysningsperiode.shouldNotBeNull()
                behandling.vilkårssett.shouldBeNull()
                behandling.stønadsdager.shouldBeNull()
            }
        }
    }
}
