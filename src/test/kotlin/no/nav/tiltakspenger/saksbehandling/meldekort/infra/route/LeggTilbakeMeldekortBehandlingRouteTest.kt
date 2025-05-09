package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taMeldekortBehanding
import org.junit.jupiter.api.Test

class LeggTilbakeMeldekortBehandlingRouteTest {
    @Test
    fun `beslutter kan legge tilbake meldekortbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _) = this.iverksett(tac)
                val beslutterIdent = "Z12345"
                val beslutter = ObjectMother.beslutter(navIdent = beslutterIdent)
                val meldekortBehandling = ObjectMother.meldekortBehandletManuelt(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    beslutter = null,
                    status = MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                    iverksattTidspunkt = null,
                )

                tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)

                taMeldekortBehanding(tac, meldekortBehandling.sakId, meldekortBehandling.id, beslutter).also {
                    val oppdatertMeldekortbehandling = tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.UNDER_BESLUTNING
                    oppdatertMeldekortbehandling?.beslutter shouldBe beslutterIdent
                }

                leggTilbakeMeldekortBehandling(tac, meldekortBehandling.sakId, meldekortBehandling.id, beslutter).also {
                    val oppdatertMeldekortbehandling = tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
                    oppdatertMeldekortbehandling?.beslutter shouldBe null
                }
            }
        }
    }

    @Test
    fun `saksbehandler kan legge tilbake meldekortbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _) = this.iverksett(tac)
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

                leggTilbakeMeldekortBehandling(tac, meldekortBehandling.sakId, meldekortBehandling.id, saksbehandler).also {
                    val oppdatertMeldekortbehandling = tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING
                    oppdatertMeldekortbehandling?.saksbehandler shouldBe null
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.leggTilbakeMeldekortBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): String {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/legg-tilbake")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ).apply {
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
