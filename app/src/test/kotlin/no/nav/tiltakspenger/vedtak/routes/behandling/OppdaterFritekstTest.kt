package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.oppdaterFritekst
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.routes
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
                val (sak, søknad) = opprettSakOgSøknad(tac)
                val behandlingId = startBehandling(tac, sak.id, søknad.id)
                val responseJson = oppdaterFritekst(tac, sak.id, behandlingId, "some_tekst")
                JSONObject(responseJson).getString("fritekstTilVedtaksbrev") shouldBe "some_tekst"
            }
        }
    }
}
