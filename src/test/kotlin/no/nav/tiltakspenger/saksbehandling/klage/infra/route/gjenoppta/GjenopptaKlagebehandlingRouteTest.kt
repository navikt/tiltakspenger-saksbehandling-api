package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandling
import org.junit.jupiter.api.Test

class GjenopptaKlagebehandlingRouteTest {
    @Test
    fun `kan gjenoppta klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgGjenopptaKlagebehandling(
                tac = tac,
            )!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = null,
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                status = "UNDER_BEHANDLING",
                ventestatus = """{"sattPåVentAv": "saksbehandlerKlagebehandling","tidspunkt": "TIMESTAMP","begrunnelse": "","erSattPåVent": false,"frist": null}""",
            )
        }
    }
}
