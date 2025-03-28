package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.fritekst

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.oppdaterFritekstForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class OppdaterFritekstTest {
    @Test
    fun `kan oppdatere fritekst`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = startBehandling(tac)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.fritekstTilVedtaksbrev?.verdi shouldBe null
                }
                val fritekstTilVedtaksbrev = "some_tekst"
                val (oppdatertSak, oppdatertBehandling, responseJson) = oppdaterFritekstForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandlingId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                )
                JSONObject(responseJson).getString("fritekstTilVedtaksbrev") shouldBe fritekstTilVedtaksbrev
                oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe fritekstTilVedtaksbrev
            }
        }
    }
}
