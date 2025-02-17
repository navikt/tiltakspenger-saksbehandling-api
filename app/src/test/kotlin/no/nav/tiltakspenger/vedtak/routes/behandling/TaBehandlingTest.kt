package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.taBehandling
import no.nav.tiltakspenger.vedtak.routes.routes
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class TaBehandlingTest {
    @Test
    fun `kan ta behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad) = opprettSakOgSøknad(tac)
                val behandlingId = startBehandling(tac, sak.id, søknad.id)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                }
                val responseJson = taBehandling(tac, behandlingId, ObjectMother.saksbehandler123())
                JSONObject(responseJson).getString("saksbehandler") shouldBe "Z12345"

                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "123"
                }
            }
        }
    }
}
