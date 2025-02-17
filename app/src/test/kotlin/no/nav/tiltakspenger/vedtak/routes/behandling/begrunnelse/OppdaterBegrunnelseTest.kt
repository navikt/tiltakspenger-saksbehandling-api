package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.oppdaterBegrunnelse
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.routes
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
                val (sak, søknad) = opprettSakOgSøknad(tac)
                val behandlingId = startBehandling(tac, sak.id, søknad.id)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.begrunnelseVilkårsvurdering?.verdi shouldBe null
                }
                val begrunnelse = "some_tekst"
                val responseJson = oppdaterBegrunnelse(tac, sak.id, behandlingId, begrunnelse)
                JSONObject(responseJson).getString("begrunnelseVilkårsvurdering") shouldBe begrunnelse
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.begrunnelseVilkårsvurdering!!.verdi shouldBe begrunnelse
                }
            }
        }
    }
}
