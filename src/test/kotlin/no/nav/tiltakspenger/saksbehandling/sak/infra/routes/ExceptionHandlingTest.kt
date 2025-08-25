package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class ExceptionHandlingTest {

    @Test
    fun `IllegalStateException skal bli til 500`() {
        val texasClientMock = mockk<TexasClient>()
        val sakService = mockk<SakService>()
        val auditServiceMock = mockk<AuditService>()
        val tilgangskontrollService = mockk<TilgangskontrollService>()
        val beslutter = ObjectMother.beslutter()
        runTest {
            coEvery { texasClientMock.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
                active = true,
                error = null,
                groups = listOf("79985315-b2de-40b8-a740-9510796993c6"),
                roles = null,
                other = mutableMapOf(
                    "azp_name" to beslutter.klientnavn,
                    "azp" to beslutter.klientId,
                    "NAVident" to beslutter.navIdent,
                    "preferred_username" to beslutter.epost,
                ),
            )
            coEvery { tilgangskontrollService.harTilgangTilPerson(any(), any(), any()) } just Runs
            coEvery { sakService.hentForFnr(any()) } throws IllegalStateException("Wuzza")

            val exceptedStatusCode = HttpStatusCode.Companion.InternalServerError
            val expectedBody =
                """
            {
              "melding": "Noe gikk galt på serversiden",
              "kode": "server_feil"
            }
                """.trimIndent()

            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClientMock)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            hentSakForFnrRoute(
                                sakService = sakService,
                                auditService = auditServiceMock,
                                clock = fixedClock,
                                tilgangskontrollService = tilgangskontrollService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Companion.Post,
                    url {
                        protocol = URLProtocol.Companion.HTTPS
                        path(SAK_PATH)
                    },
                ) {
                    setBody("""{"fnr": "12345678901"}""")
                }.apply {
                    status shouldBe exceptedStatusCode
                    contentType() shouldBe ContentType.Companion.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        expectedBody,
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
            }
        }
    }
}
