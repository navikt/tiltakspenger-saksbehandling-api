package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvis

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgForhåndsvisKlagebehandlingsbrev
import org.junit.jupiter.api.Test

class ForhåndsvisBrevKlagebehandlingRouteTest {
    @Test
    fun `kan forhåndsvise klagebehandling til avvisning `() {
        withTestApplicationContext { tac ->
            opprettSakOgForhåndsvisKlagebehandlingsbrev(
                tac = tac,
            )!!
        }
    }
}
