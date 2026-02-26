package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent
import org.junit.jupiter.api.Test

class SettKlagebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent(
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
                ventestatus = """{"sattPåVentAv": "saksbehandlerKlagebehandling","tidspunkt": "2025-01-01T01:02:38.456789","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}""",
                hjemler = null,
                klageinstanshendelser = null,
            )
        }
    }
}
