package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.nonEmptyListOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
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
                sendRevurderingStansTilBeslutningForBehandlingId(
                    tac,
                    sak.id,
                    revurdering.id,
                    stansperiode = søknadsbehandling.virkningsperiode!!,
                    valgteHjemler = nonEmptyListOf("Alder"),
                )
                taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
                iverksettForBehandlingId(tac, sak.id, revurdering.id)
            }
        }
    }

    @Test
    fun `kan iverksette revurdering innvilgelsesperiode fremover`() {
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

                sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
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

    @Test
    fun `kan iverksette revurdering innvilgelsesperiode bakover`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
                val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.minusFraOgMed(14L)

                val (sak, _, søknadsbehandling, revurdering) = startRevurderingInnvilgelse(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                    revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
                )

                sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
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
