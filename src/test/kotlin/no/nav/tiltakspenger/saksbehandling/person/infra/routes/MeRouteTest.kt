package no.nav.tiltakspenger.saksbehandling.person.infra.routes

import arrow.core.right
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.SAKSBEHANDLER_PATH
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.meRoute
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class MeRouteTest {
    private val tokenService = object : TokenService {
        override suspend fun validerOgHentBruker(token: String) = saksbehandler().right()
    }

    // language = JSON
    private val saksbehandlerMock =
        """
        {
          "navIdent":"Z12345",
          "brukernavn":"Sak Behandler",
          "epost":"Sak.Behandler@nav.no",
          "roller":["SAKSBEHANDLER"]
        }
        """.trimIndent()

    @Test
    fun test() {
        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meRoute(
                            tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(SAKSBEHANDLER_PATH)
                    },
                ).apply {
                    withClue(
                        "Response details:\n" +
                            "Status: ${this.status}\n" +
                            "Content-Type: ${this.contentType()}\n" +
                            "Body: ${this.bodyAsText()}\n",
                    ) {
                        status shouldBe HttpStatusCode.OK
                        contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                        JSONAssert.assertEquals(
                            saksbehandlerMock,
                            bodyAsText(),
                            JSONCompareMode.LENIENT,
                        )
                    }
                }
            }
        }
    }
}
