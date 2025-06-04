package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
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
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.nySøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import org.junit.jupiter.api.Test

class HarSoknadUnderBehandlingRouteTest {
    @Test
    fun `har åpen søknad uten behandling - returnerer true`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val fnr = Fnr.random()
                tac.nySøknad(fnr = fnr)
                harSoknadUnderBehandling(tac, fnr).also {
                    it shouldBe """{"harSoknadUnderBehandling":true}"""
                }
            }
        }
    }

    @Test
    fun `har søknad med åpen behandling - returnerer true`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val fnr = Fnr.random()
                startBehandling(tac, fnr = fnr)
                harSoknadUnderBehandling(tac, fnr).also {
                    it shouldBe """{"harSoknadUnderBehandling":true}"""
                }
            }
        }
    }

    @Test
    fun `har søknad med iverksatt behandling - returnerer false`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val fnr = Fnr.random()
                iverksettSøknadsbehandling(tac, fnr = fnr)
                harSoknadUnderBehandling(tac, fnr).also {
                    it shouldBe """{"harSoknadUnderBehandling":false}"""
                }
            }
        }
    }

    @Test
    fun `har ingen søknader - returnerer false`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val fnr = Fnr.random()
                harSoknadUnderBehandling(tac, fnr).also {
                    it shouldBe """{"harSoknadUnderBehandling":false}"""
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.harSoknadUnderBehandling(
        tac: TestApplicationContext,
        fnr: Fnr,
    ): String {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/har-soknad")
            },
            jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("lage_hendelser"),
            ),
        ) { setBody("""{"fnr": "${fnr.verdi}"}""") }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return bodyAsText
        }
    }
}
