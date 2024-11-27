package no.nav.tiltakspenger.vedtak.routes.behandling

import arrow.core.right
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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.benk.behandlingBenkRoutes
import no.nav.tiltakspenger.vedtak.routes.configureExceptions
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class ExceptionHandlingTest {

    @Test
    fun `IllegalStateException skal bli til 500`() {
        val tokenServiceMock = mockk<TokenService>()
        val sakService = mockk<SakService>()
        val mockAuditService = mockk<AuditService>()
        runTest {
            coEvery { tokenServiceMock.validerOgHentBruker(any()) } returns ObjectMother.beslutter().right()
            coEvery { sakService.hentSaksoversikt(any(), any()) } throws IllegalStateException("Wuzza")

            val exceptedStatusCode = HttpStatusCode.InternalServerError
            val expectedBody =
                """
            {
              "melding": "Noe gikk galt på serversiden",
              "kode": "server_feil"
            }
                """.trimIndent()

            testApplication {
                application {
                    // vedtakTestApi()
                    jacksonSerialization()
                    configureExceptions()
                    routing {
                        behandlingBenkRoutes(
                            tokenService = tokenServiceMock,
                            behandlingService = mockk<BehandlingServiceImpl>(),
                            sakService = sakService,
                            auditService = mockAuditService,
                            startRevurderingService = mockk(),
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(BEHANDLINGER_PATH)
                    },
                ).apply {
                    status shouldBe exceptedStatusCode
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
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
