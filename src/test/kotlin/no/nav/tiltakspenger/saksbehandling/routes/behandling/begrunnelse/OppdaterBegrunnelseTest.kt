package no.nav.tiltakspenger.saksbehandling.routes.behandling.begrunnelse

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.oppdaterBegrunnelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.saksbehandling.routes.routes
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class OppdaterBegrunnelseTest {
    @Test
    fun `kan starte behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = startBehandling(tac)
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.begrunnelseVilkårsvurdering?.verdi shouldBe null
                }
                val begrunnelse = "some_tekst"
                val (oppdatertSak, oppdatertBehandling, responseJson) = oppdaterBegrunnelseForBehandlingId(
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
