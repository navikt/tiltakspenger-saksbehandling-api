package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksett
import org.json.JSONObject
import org.junit.jupiter.api.Test

class OvertaMeldekortBehandlingRouteTest {
    @Test
    fun `saksbehandler kan overta meldekortbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _) = this.iverksett(tac)
                val saksbehandlerIdent = "Z12345"
                val meldekortBehandling = ObjectMother.meldekortUnderBehandling(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    saksbehandler = saksbehandlerIdent,
                )

                tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling)

                overtaMeldekortBehandling(tac, meldekortBehandling.sakId, meldekortBehandling.id, saksbehandlerIdent, ObjectMother.saksbehandler123()).also {
                    JSONObject(it).getString("saksbehandler") shouldBe "123"
                    val oppdatertMeldekortbehandling = tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING
                    oppdatertMeldekortbehandling?.saksbehandler shouldBe "123"
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.overtaMeldekortBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        overtarFra: String,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): String {
        defaultRequest(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/overta")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ) {
            this.setBody("""{"overtarFra":"$overtarFra"}""")
        }.apply {
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
