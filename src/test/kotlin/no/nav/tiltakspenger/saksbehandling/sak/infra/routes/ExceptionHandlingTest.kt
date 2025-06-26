package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class ExceptionHandlingTest {

    @Test
    fun `IllegalStateException skal bli til 500`() {
        val tokenServiceMock = mockk<TokenService>()
        val sakService = mockk<SakService>()
        val auditServiceMock = mockk<AuditService>()
        runTest {
            coEvery { tokenServiceMock.validerOgHentBruker(any()) } returns ObjectMother.beslutter().right()
            coEvery { sakService.hentForFnr(any(), any(), any()) } throws IllegalStateException("Wuzza")

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
                    routing {
                        hentSakForFnrRoute(
                            tokenService = tokenServiceMock,
                            sakService = sakService,
                            auditService = auditServiceMock,
                            clock = fixedClock,
                        )
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
