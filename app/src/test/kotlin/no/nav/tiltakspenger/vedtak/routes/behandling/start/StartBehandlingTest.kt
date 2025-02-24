package no.nav.tiltakspenger.vedtak.routes.behandling.start

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

internal class StartBehandlingTest {
    @Test
    fun `kan starte behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = startBehandling(tac)
                val behandlingId = behandling.id
                val opprettetBehandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)
                opprettetBehandling.erFørstegangsbehandling shouldBe true
                opprettetBehandling.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                opprettetBehandling.sakId shouldBe sak.id
                opprettetBehandling.oppgaveId shouldBe søknad.oppgaveId
                opprettetBehandling.fritekstTilVedtaksbrev shouldBe null
                opprettetBehandling.begrunnelseVilkårsvurdering shouldBe null
                opprettetBehandling.saksbehandler shouldBe "Z12345"
                opprettetBehandling.saksnummer shouldBe sak.saksnummer
                opprettetBehandling.søknad!!.id shouldBe søknad.id
                opprettetBehandling.attesteringer shouldBe emptyList()
                opprettetBehandling.erRevurdering shouldBe false
                opprettetBehandling.saksopplysninger.shouldNotBeNull()
            }
        }
    }
}
