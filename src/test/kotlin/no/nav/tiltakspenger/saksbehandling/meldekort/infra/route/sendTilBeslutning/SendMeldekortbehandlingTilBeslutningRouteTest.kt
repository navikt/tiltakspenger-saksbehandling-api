package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.sendTilBeslutning

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import org.junit.jupiter.api.Test

internal class SendMeldekortbehandlingTilBeslutningRouteTest {
    @Test
    fun `kan sende meldekortbehandling til beslutter`() {
        runTest {
            withTestApplicationContext { tac ->
                this.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
                    tac = tac,
                    saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
                )!!
            }
        }
    }
}
