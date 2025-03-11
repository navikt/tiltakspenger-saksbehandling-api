package no.nav.tiltakspenger.saksbehandling.routes.revurdering

import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendRevurderingTilBeslutterForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.routes.routes
import org.junit.jupiter.api.Test

internal class IverksettRevurderingTest {
    @Test
    fun `kan iverksette revurdering`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, førstegangsbehandling, revurdering) = startRevurdering(tac)
                taBehanding(tac, revurdering.id)
                sendRevurderingTilBeslutterForBehandlingId(tac, sak.id, revurdering.id, stansperiode = førstegangsbehandling.virkningsperiode!!)
                taBehanding(tac, revurdering.id, saksbehandler = ObjectMother.beslutter())
                iverksettForBehandlingId(tac, sak.id, revurdering.id)
            }
        }
    }
}
