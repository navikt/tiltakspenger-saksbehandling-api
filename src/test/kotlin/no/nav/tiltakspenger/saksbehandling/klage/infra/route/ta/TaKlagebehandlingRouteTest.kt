package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandling
import org.junit.jupiter.api.Test

class TaKlagebehandlingRouteTest {
    @Test
    fun `kan ta klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgTaKlagebehandling(
                tac = tac,
            )!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerSomTar",
                resultat = null,
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                status = "UNDER_BEHANDLING",
            )
        }
    }

    @Test
    fun `idempotent og ta klagebehandling fra seg selv`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar")
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgTaKlagebehandling(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
                saksbehandlerSomTar = saksbehandler,
            )!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerSomOvertar",
                resultat = null,
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                status = "UNDER_BEHANDLING",
            )
        }
    }
}
