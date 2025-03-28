package no.nav.tiltakspenger.saksbehandling.routes.revurdering

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.routes
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus
import org.junit.jupiter.api.Test

internal class StartRevurderingTest {
    @Test
    fun `kan starte revurdering`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, førstegangsbehandling, revurdering) = startRevurdering(tac)
                revurdering.erFørstegangsbehandling shouldBe false
                revurdering.erRevurdering shouldBe true
                revurdering.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                revurdering.sakId shouldBe sak.id
                revurdering.oppgaveId shouldBe null
                revurdering.fritekstTilVedtaksbrev shouldBe null
                revurdering.begrunnelseVilkårsvurdering shouldBe null
                revurdering.saksbehandler shouldBe "Z12345"
                revurdering.saksnummer shouldBe sak.saksnummer
                revurdering.søknad.shouldBeNull()
                revurdering.virkningsperiode shouldBe null
                revurdering.attesteringer shouldBe emptyList()
                revurdering.saksopplysninger.shouldNotBeNull()
            }
        }
    }
}
