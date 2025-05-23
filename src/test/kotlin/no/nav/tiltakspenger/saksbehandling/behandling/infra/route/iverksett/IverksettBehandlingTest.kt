package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksett
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksettForBehandlingIdReturnerRespons
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import org.junit.jupiter.api.Test

class IverksettBehandlingTest {
    @Test
    fun `send til beslutter endrer status på behandlingen`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (_, _, behandling) = this.iverksett(tac)
                behandling.virkningsperiode.shouldNotBeNull()
                behandling.status shouldBe Behandlingsstatus.VEDTATT
            }
        }
    }

    @Test
    fun `iverksett - feilmelding hvis behandlingen ikke er tildelt beslutter`() = runTest {
        with(TestApplicationContext()) {
            val saksbehandler = saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.startBehandling(tac, saksbehandler = saksbehandler)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                }
                sendSøknadsbehandlingTilBeslutningForBehandlingId(
                    tac,
                    sak.id,
                    behandlingId,
                    saksbehandler,
                    innvilgelsesperiode = søknad.vurderingsperiode(),
                    eksternDeltagelseId = søknad.tiltak.id,
                )
                taBehanding(tac, sak.id, behandlingId, beslutter)

                val response = iverksettForBehandlingIdReturnerRespons(tac, sak.id, behandlingId, ObjectMother.beslutter(navIdent = "B999999"))

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe objectMapper.writeValueAsString(Standardfeil.behandlingenEiesAvAnnenSaksbehandler(beslutter.navIdent))
            }
        }
    }
}
