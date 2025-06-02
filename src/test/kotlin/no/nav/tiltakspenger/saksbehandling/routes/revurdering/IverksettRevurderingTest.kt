package no.nav.tiltakspenger.saksbehandling.routes.revurdering

import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendRevurderingInnvilgelseTilBeslutterForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendRevurderingTilBeslutterForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import org.junit.jupiter.api.Test

internal class IverksettRevurderingTest {
    @Test
    fun `kan iverksette revurdering stans`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, søknadsbehandling, revurdering) = startRevurderingStans(tac)
                sendRevurderingTilBeslutterForBehandlingId(
                    tac,
                    sak.id,
                    revurdering.id,
                    stansperiode = søknadsbehandling.virkningsperiode!!,
                    valgteHjemler = listOf("Alder"),
                )
                taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
                iverksettForBehandlingId(tac, sak.id, revurdering.id)
            }
        }
    }

    @Test
    fun `kan iverksette revurdering innvilgelse`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
                val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L)

                val (sak, _, søknadsbehandling, revurdering) = startRevurderingInnvilgelse(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                    revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
                )

                sendRevurderingInnvilgelseTilBeslutterForBehandlingId(
                    tac,
                    sak.id,
                    revurdering.id,
                    innvilgelsesperiode = revurderingInnvilgelsesperiode,
                    eksternDeltagelseId = søknadsbehandling.søknad.tiltak.id,
                )
                taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
                iverksettForBehandlingId(tac, sak.id, revurdering.id)
            }
        }
    }
}
