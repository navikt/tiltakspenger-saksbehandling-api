package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingVilkårsvurdert
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.sendTilBeslutter
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

class SendTilBeslutterTest {
    @Test
    fun `send til beslutter endrer status på behandlingen`() = runTest {
        with(TestApplicationContext()) {
            val saksbehandler = saksbehandler()
            val tac = this
            val sak = this.førstegangsbehandlingVilkårsvurdert(saksbehandler = saksbehandler)
            val behandlingId = sak.førstegangsbehandling!!.id
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                this.sendTilBeslutter(tac, behandlingId)
            }

            val behandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)

            behandling.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
            behandling.saksbehandler shouldBe saksbehandler.navIdent
        }
    }
}
