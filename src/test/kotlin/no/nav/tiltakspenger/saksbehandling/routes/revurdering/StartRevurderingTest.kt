package no.nav.tiltakspenger.saksbehandling.routes.revurdering

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurdering
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
                revurdering.shouldBeInstanceOf<Revurdering>()
                revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
                revurdering.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                revurdering.sakId shouldBe sak.id
                revurdering.oppgaveId shouldBe null
                revurdering.fritekstTilVedtaksbrev shouldBe null
                revurdering.begrunnelseVilkårsvurdering shouldBe null
                revurdering.saksbehandler shouldBe "Z12345"
                revurdering.saksnummer shouldBe sak.saksnummer
                revurdering.virkningsperiode shouldBe null
                revurdering.attesteringer shouldBe emptyList()
                revurdering.saksopplysninger.shouldNotBeNull()
            }
        }
    }
}
