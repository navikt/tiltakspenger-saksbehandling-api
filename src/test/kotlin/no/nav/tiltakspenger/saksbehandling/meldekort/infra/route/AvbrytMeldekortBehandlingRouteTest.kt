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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvbrytMeldekortBehandlingRouteTest {
    @Test
    fun `saksbehandler kan avbryte meldekortbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _) = this.iverksettSøknadsbehandling(tac)
                val saksbehandlerIdent = "Z12345"
                val saksbehandler = ObjectMother.saksbehandler(navIdent = saksbehandlerIdent)
                val meldekortBehandling = ObjectMother.meldekortUnderBehandling(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    saksbehandler = saksbehandlerIdent,
                    status = MeldekortBehandlingStatus.UNDER_BEHANDLING,
                )

                tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)

                val begrunnelse = "begrunnelse"

                avbrytMeldekortBehandling(
                    tac,
                    meldekortBehandling.sakId,
                    meldekortBehandling.id,
                    begrunnelse,
                    saksbehandler,
                ).also {
                    val oppdatertMeldekortbehandling =
                        tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.AVBRUTT
                    oppdatertMeldekortbehandling?.avbrutt?.saksbehandler shouldBe saksbehandlerIdent
                    oppdatertMeldekortbehandling?.avbrutt?.tidspunkt?.toLocalDate() shouldBe LocalDate.now()
                    oppdatertMeldekortbehandling?.avbrutt?.begrunnelse shouldBe begrunnelse

                    val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                    oppdatertSak.meldekortBehandlinger.ikkeAvbrutteMeldekortBehandlinger shouldBe emptyList()
                    oppdatertSak.meldekortBehandlinger.avbrutteMeldekortBehandlinger.size shouldBe 1
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.avbrytMeldekortBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        begrunnelse: String,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): String {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/avbryt")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ) {
            this.setBody("""{"begrunnelse":"$begrunnelse"}""")
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
