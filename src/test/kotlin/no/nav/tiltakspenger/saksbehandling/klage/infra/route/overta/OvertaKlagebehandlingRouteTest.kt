package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOvertaKlagebehandling
import org.junit.jupiter.api.Test

class OvertaKlagebehandlingRouteTest {
    @Test
    fun `kan overta klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgOvertaKlagebehandling(
                tac = tac,
            )!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerSomOvertar",
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                hjemler = null,
                klageinstanshendelser = null,
            )
        }
    }

    @Test
    fun `idempotent og overta klagebehandling fra seg selv`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar")
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgOvertaKlagebehandling(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
                saksbehandlerSomOvertar = saksbehandler,
            )!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerSomOvertar",
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                hjemler = null,
                klageinstanshendelser = null,
            )
        }
    }
}
