package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.withClue
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
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

internal class SendMeldekortBehandlingTilBeslutterRouteTest {

    @Test
    fun `for mange meldedager`() {
        val sakId = SakId.random()
        val meldekortId = MeldekortId.random()
        val tokenService = object : TokenService {
            override suspend fun validerOgHentBruker(token: String) = ObjectMother.saksbehandler().right()
        }
        val auditService = mockk<AuditService>()
        val oppdaterMeldekortService = mockk<OppdaterMeldekortService>()
        coEvery { auditService.logMedMeldekortId(any(), any(), any(), any(), any()) } returns Unit
        coEvery {
            oppdaterMeldekortService.sendMeldekortTilBeslutter(any())
        } returns KanIkkeOppdatereMeldekort.ForMangeDagerUtfylt(14, 15).left()
        val request = """
            {
              "dager": [
                {"dato":"2024-01-01","status":"FRAVÆR_SYK"}
              ]
            }
        """.trimIndent()
        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        sendMeldekortTilBeslutterRoute(
                            tokenService = tokenService,
                            auditService = auditService,
                            oppdaterMeldekortService = oppdaterMeldekortService,
                            clock = fixedClock,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/sak/$sakId/meldekort/$meldekortId")
                    },
                ) {
                    setBody(request)
                }.apply {
                    withClue(
                        "Response details:\n" +
                            "Status: ${this.status}\n" +
                            "Content-Type: ${this.contentType()}\n" +
                            "Body: ${this.bodyAsText()}\n",
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                        contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                        bodyAsText() shouldBe """{"melding":"Kan ikke sende meldekort til beslutter. For mange dager er utfylt. Maks antall for dette meldekortet er 14, mens antall utfylte dager er 15.","kode":"for_mange_dager_utfylt"}"""
                    }
                }
            }
        }
    }
}
