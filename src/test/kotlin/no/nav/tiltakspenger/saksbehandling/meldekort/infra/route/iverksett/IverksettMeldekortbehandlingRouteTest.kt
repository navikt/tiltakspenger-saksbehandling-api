package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksett

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import org.junit.jupiter.api.Test

class IverksettMeldekortbehandlingRouteTest {
    @Test
    fun `saksbehandler kan iverksette meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, _, _) = this.iverksettSøknadsbehandlingOgMeldekortbehandling(
                tac = tac,
            )!!
        }
    }
}
