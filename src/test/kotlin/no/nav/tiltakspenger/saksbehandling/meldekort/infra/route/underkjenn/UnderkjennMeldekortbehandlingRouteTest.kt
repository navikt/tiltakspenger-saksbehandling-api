package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjenn

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling
import org.junit.jupiter.api.Test

class UnderkjennMeldekortbehandlingRouteTest {
    @Test
    fun `saksbehandler kan underkjenne meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, _, _) = this.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling(
                tac = tac,
            )!!
        }
    }
}
