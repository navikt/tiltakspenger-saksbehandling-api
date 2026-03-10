package no.nav.tiltakspenger.saksbehandling.klage.infra.route.innstillingsbrev

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgVisinnstillingsbrevForKlagebehandling
import org.junit.jupiter.api.Test

class VisInnstillingsbrevKlagebehandlingRouteTest {

    @Test
    fun `kan se innstillingsbrev for klage`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            opprettSakOgVisinnstillingsbrevForKlagebehandling(tac = tac)!!
        }
    }
}
