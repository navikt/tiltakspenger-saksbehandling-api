package no.nav.tiltakspenger.vedtak.routes.behandling.start

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.nySøknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

/** TODO John + Anders: Slettes når vi fjerner den gamle behandlingsflyten. */
class StartDeprecatedBehandlingTest {
    @Test
    fun `startbehandling - oppretter førstegangsbehandling knyttet til søknad og sak`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = ObjectMother.nySak()
            tac.sakContext.sakRepo.opprettSak(sak)
            val søknad = this.nySøknad(
                fnr = sak.fnr,
                sak = sak,
            )

            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/startbehandling")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ) {
                    setBody("""{"id":"${søknad.id}"}""")
                }.apply {
                    withClue(
                        "Response details:\n" +
                            "Status: ${this.status}\n" +
                            "Content-Type: ${this.contentType()}\n" +
                            "Body: ${this.bodyAsText()}\n",
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }
                }
                val opprettetBehandling = tac.behandlingContext.behandlingRepo.hentForSøknadId(søknad.id)!!
                opprettetBehandling.erFørstegangsbehandling shouldBe true
                opprettetBehandling.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                opprettetBehandling.sakId shouldBe sak.id
                opprettetBehandling.oppgaveId shouldBe søknad.oppgaveId
                opprettetBehandling.erNyFlyt shouldBe false
            }
        }
    }
}
