package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.leggTilbake

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import org.json.JSONObject
import org.junit.jupiter.api.Test

class LeggTilbakeBehandlingRouteTest {
    @Test
    fun `saksbehandler kan legge tilbake behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = startBehandling(tac)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                }
                leggTilbakeBehanding(tac, sak.id, behandlingId, ObjectMother.saksbehandler()).also {
                    JSONObject(it).getString("saksbehandler") shouldBe "null"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.KLAR_TIL_BEHANDLING
                        it.saksbehandler shouldBe null
                    }
                }
            }
        }
    }

    @Test
    fun `beslutter kan legge tilbake behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                }
                taBehanding(tac, sak.id, behandlingId, ObjectMother.beslutter()).also {
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B12345"
                    }
                }
                leggTilbakeBehanding(tac, sak.id, behandlingId, ObjectMother.beslutter()).also {
                    JSONObject(it).getString("beslutter") shouldBe "null"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                        it.beslutter shouldBe null
                    }
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.leggTilbakeBehanding(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): String {
        defaultRequest(
            HttpMethod.Companion.Post,
            url {
                protocol = URLProtocol.Companion.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/legg-tilbake")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.Companion.OK
            }
            return bodyAsText
        }
    }
}
