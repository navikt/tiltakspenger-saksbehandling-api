package no.nav.tiltakspenger.vedtak.routes.revurdering

import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.sendTilBeslutterForBehandlingId
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startRevurdering
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.taBehanding
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

internal class IverksettRevurderingTest {
    @Test
    fun `kan starte revurdering`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurdering(tac)
                taBehanding(tac, revurdering.id)
                sendTilBeslutterForBehandlingId(tac, sak.id, revurdering.id)
                taBehanding(tac, revurdering.id, saksbehandler = ObjectMother.beslutter())
                iverksettForBehandlingId(tac, sak.id, revurdering.id)
            }
        }
    }
}
