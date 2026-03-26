package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppretthold

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Test

class OpprettholdKlagebehandlingRouteTest {
    @Test
    fun `kan opprettholde klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val rammevedtakDetKlagesPå = sak.rammevedtaksliste.first()
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                status = "OPPRETTHOLDT",
                resultat = "OPPRETTHOLDT",
                kanIverksetteVedtak = null,
                vedtakDetKlagesPå = "${rammevedtakDetKlagesPå.id}",
                behandlingDetKlagesPå = "${rammevedtakDetKlagesPå.behandlingId}",
                brevtekst = listOf(
                    """{"tittel": "Hva klagesaken gjelder","tekst": "Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"}""",
                    """{"tittel": "Klagers anførsler","tekst": "<saksbehandler fyller ut>"}""",
                    """{"tittel": "Vurdering av klagen","tekst": "<saksbehandler fyller ut>"}""",
                ),
                hjemler = listOf("ARBEIDSMARKEDSLOVEN_17"),
                iverksattOpprettholdelseTidspunkt = true,
            )
        }
    }
}
