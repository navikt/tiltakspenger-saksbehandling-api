package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import org.junit.jupiter.api.Test

class BehandlingBeslutterRoutesTest {

    @Test
    fun `sjekk at begrunnelse kan sendes inn`() = runTest {
        with(TestApplicationContext()) {
            val beslutter = beslutter()
            val tac = this
            val sak = this.førstegangsbehandlingUnderBeslutning(beslutter = beslutter)
            val behandlingId = sak.førstegangsbehandling!!.id
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        behandlingBeslutterRoutes(
                            behandlingService = tac.behandlingContext.behandlingService,
                            auditService = tac.personContext.auditService,
                            tokenService = tac.tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/sendtilbake/$behandlingId")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = beslutter),
                ) {
                    setBody("""{"begrunnelse": "begrunnelse"}""")
                }.apply {
                    status shouldBe HttpStatusCode.OK
                }
            }
            tac.behandlingContext.behandlingRepo.hent(behandlingId).attesteringer.single().let {
                it shouldBe
                    Attestering(
                        // Ignorerer id+tidspunkt
                        id = it.id,
                        tidspunkt = it.tidspunkt,
                        status = no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus.SENDT_TILBAKE,
                        begrunnelse = "begrunnelse",
                        beslutter = beslutter().navIdent,
                    )
            }
        }
    }
}
