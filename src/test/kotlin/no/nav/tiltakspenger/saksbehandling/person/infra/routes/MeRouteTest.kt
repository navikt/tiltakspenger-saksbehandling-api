package no.nav.tiltakspenger.saksbehandling.person.infra.routes

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.SAKSBEHANDLER_PATH
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.meRoute
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class MeRouteTest {
    val texasClient = mockk<TexasClient>()

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
    fun `get saksbehandler - er saksbehandler med gyldig token - returnerer saksbehandler`() {
        coEvery { texasClient.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
            roles = null,
            other = mutableMapOf(
                "azp_name" to "saksbehandling",
                "azp" to "saksbehandling-id",
                "NAVident" to "Z12345",
                "preferred_username" to "Sak.Behandler@nav.no",
            ),
        )
        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            meRoute()
                        }
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

    @Test
    fun `get saksbehandler - utløpt token - returnerer 401`() {
        coEvery { texasClient.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
            active = false,
            error = null,
            groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
            roles = null,
            other = mutableMapOf(
                "azp_name" to "saksbehandling",
                "azp" to "saksbehandling-id",
                "NAVident" to "Z12345",
                "preferred_username" to "Sak.Behandler@nav.no",
            ),
        )
        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            meRoute()
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(SAKSBEHANDLER_PATH)
                    },
                ).apply {
                    status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }
    }

    @Test
    fun `get saksbehandler - ugyldig token - returnerer 403`() {
        coEvery { texasClient.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
            roles = null,
            other = mutableMapOf(
                "azp_name" to "saksbehandling",
                "azp" to "saksbehandling-id",
            ),
        )
        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            meRoute()
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(SAKSBEHANDLER_PATH)
                    },
                ).apply {
                    status shouldBe HttpStatusCode.Forbidden
                }
            }
        }
    }
}
