package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
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
                opprettetBehandling.shouldBeInstanceOf<Søknadsbehandling>()
                opprettetBehandling.behandlingstype shouldBe Behandlingstype.SØKNADSBEHANDLING
                opprettetBehandling.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                opprettetBehandling.sakId shouldBe sak.id
                opprettetBehandling.oppgaveId shouldBe søknad.oppgaveId
                opprettetBehandling.fritekstTilVedtaksbrev shouldBe null
                opprettetBehandling.begrunnelseVilkårsvurdering shouldBe null
                opprettetBehandling.saksbehandler shouldBe "Z12345"
                opprettetBehandling.saksnummer shouldBe sak.saksnummer
                opprettetBehandling.søknad.id shouldBe søknad.id
                opprettetBehandling.attesteringer shouldBe emptyList()
                opprettetBehandling.saksopplysninger.shouldNotBeNull()
            }
        }
    }
}
