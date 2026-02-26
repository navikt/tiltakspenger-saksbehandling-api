package no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingTilbake
import org.junit.jupiter.api.Test

class LeggTilbakeKlagebehandlingRouteTest {
    @Test
    fun `kan legge klagebehandlingen tilbake`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgLeggKlagebehandlingTilbake(
                tac = tac,
            )!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = null,
                status = "KLAR_TIL_BEHANDLING",
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                hjemler = null,
                klageinstanshendelser = null,
            )
        }
    }
}
