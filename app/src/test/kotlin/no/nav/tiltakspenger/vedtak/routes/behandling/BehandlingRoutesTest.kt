package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingVilkårsvurdert
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

class BehandlingRoutesTest {
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
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/beslutter/$behandlingId")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                }
            }

            val behandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)

            behandling.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
            behandling.saksbehandler shouldBe saksbehandler.navIdent
        }
    }
}
