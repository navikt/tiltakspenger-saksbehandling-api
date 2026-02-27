package no.nav.tiltakspenger.saksbehandling.klage.infra.route.start

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import org.junit.jupiter.api.Test

class OpprettKlagebehandlingRouteTest {
    @Test
    fun `kan opprette klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val fnr = Fnr.fromString("12345678912")
            val (sak, klagebehandling, json) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac, fnr = fnr)!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                resultat = "AVVIST",
                status = "UNDER_BEHANDLING",
            )
        }
    }
}
