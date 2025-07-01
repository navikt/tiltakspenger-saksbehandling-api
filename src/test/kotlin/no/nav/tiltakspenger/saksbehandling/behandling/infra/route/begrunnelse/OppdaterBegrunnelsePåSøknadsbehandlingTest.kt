package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.begrunnelse

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBegrunnelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class OppdaterBegrunnelsePåSøknadsbehandlingTest {
    @Test
    fun `kan oppdatere begrunnelse på søknadsbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.begrunnelseVilkårsvurdering?.verdi shouldBe null
                }
                val begrunnelse = "some_tekst"
                val (_, oppdatertBehandling, responseJson) = oppdaterBegrunnelseForBehandlingId(
                    tac,
                    sak.id,
                    behandling.id,
                    begrunnelse = begrunnelse,
                )
                JSONObject(responseJson).getString("begrunnelseVilkårsvurdering") shouldBe begrunnelse
                oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe begrunnelse
            }
        }
    }
}
