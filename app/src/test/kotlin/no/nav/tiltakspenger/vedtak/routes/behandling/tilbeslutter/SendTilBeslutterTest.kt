package no.nav.tiltakspenger.vedtak.routes.behandling.tilbeslutter

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.sendTilBeslutterForBehandlingId
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

class SendTilBeslutterTest {
    @Test
    fun `send til beslutter endrer status på behandlingen`() = runTest {
        with(TestApplicationContext()) {
            val saksbehandler = saksbehandler()
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandlingId) = this.startBehandling(tac, saksbehandler = saksbehandler)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                }
                sendTilBeslutterForBehandlingId(tac, sak.id, behandlingId, saksbehandler)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                }
            }
        }
    }
}
