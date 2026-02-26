package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingBrevtekstRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, behandling, json) = opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = behandling.sakId,
                saksnummer = Saksnummer("202505011001"),
                klagebehandlingId = behandling.id,
                fnr = "12345678911",
                resultat = "AVVIST",
                brevtekst = listOf("""{"tittel": "Avvisning av klage","tekst": "Din klage er dessverre avvist."}"""),
                kanIverksetteVedtak = true,
                hjemler = null,
                klageinstanshendelser = null,
            )
        }
    }

    @Test
    fun `kan oppdatere klagebehandling (opprettholdelse) - brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, rammevedtak, klagebehandling, json) = opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = klagebehandling.sakId,
                saksnummer = Saksnummer("202505011001"),
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = "${rammevedtak.id}",
                brevtekst = listOf(
                    """{"tittel": "Hva klagesaken gjelder","tekst": "Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"}""",
                    """{"tittel": "Klagers anførsler","tekst": "<saksbehandler fyller ut>"}""",
                    """{"tittel": "Vurdering av klagen","tekst": "<saksbehandler fyller ut>"}""",
                ),
                kanIverksetteOpprettholdelse = true,
                hjemler = listOf("ARBEIDSMARKEDSLOVEN_17"),
            )
        }
    }
}
