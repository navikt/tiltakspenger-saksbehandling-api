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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

internal class SendMeldekortBehandlingTilBeslutterRouteTest {

    @Test
    fun `meldekortperioden kan ikke være frem i tid`() {
        val sakId = SakId.random()
        val meldekortId = MeldekortId.random()
        val tokenService = object : TokenService {
            override suspend fun validerOgHentBruker(token: String) = ObjectMother.saksbehandler().right()
        }
        val auditService = mockk<AuditService>()
        val sendMeldekortTilBeslutterService = mockk<SendMeldekortTilBeslutterService>()
        coEvery { auditService.logMedMeldekortId(any(), any(), any(), any(), any()) } returns Unit
        coEvery {
            sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(any())
        } returns KanIkkeSendeMeldekortTilBeslutter.KanIkkeOppdatere(KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid).left()
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
                            sendMeldekortTilBeslutterService = sendMeldekortTilBeslutterService,
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
                        bodyAsText() shouldBe """{"melding":"Kan ikke sende inn et meldekort før meldekortperioden har begynt.","kode":"meldekortperioden_kan_ikke_være_frem_i_tid"}"""
                    }
                }
            }
        }
    }
}
