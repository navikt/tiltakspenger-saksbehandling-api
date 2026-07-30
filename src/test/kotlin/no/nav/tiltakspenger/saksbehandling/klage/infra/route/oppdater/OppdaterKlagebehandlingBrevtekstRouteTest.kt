package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterKlagebehandlingBrevtekstForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taKlagebehandling
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingBrevtekstRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - brevtekst`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling, json) = opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = behandling.sakId,
                saksnummer = sak.saksnummer,
                klagebehandlingId = behandling.id,
                fnr = sak.fnr.verdi,
                resultat = "AVVIST",
                brevtekst = listOf("""{"tittel": "Avvisning av klage","tekst": "Din klage er dessverre avvist."}"""),
                kanIverksetteVedtak = true,
            )
        }
    }

    @Test
    fun `kan oppdatere klagebehandling (opprettholdelse) - brevtekst`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, rammevedtak, klagebehandling, json) = opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = klagebehandling.sakId,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = sak.fnr.verdi,
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = "${rammevedtak.id}",
                behandlingDetKlagesPå = "${rammevedtak.behandlingId}",
                kanIverksetteVedtak = null,
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

    @Test
    fun `skal ikke kunne oppdatere brevtekst dersom klagebehandlingen er satt på vent, og saksbehandler er på behandlingen`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val (sak, _, _, klagebehandling) = iverksettSøknadsbehandlingOgVurderKlagebehandlingTilOpprettholdelse(tac)!!

            settKlagebehandlingPåVent(tac = tac, sakId = sak.id, klagebehandlingId = klagebehandling.id)

            taKlagebehandling(tac = tac, sakId = sak.id, klagebehandlingId = klagebehandling.id)

            oppdaterKlagebehandlingBrevtekstForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventet = ForventetRespons.json(
                    400, //language=json
                    """{"kode": "behandlingen_er_satt_på_vent", "melding": "Kan ikke oppdatere brevtekst fordi klagebehandlingen er satt på vent"}""",
                    "application/json; charset=UTF-8",
                ),
            )
        }
    }
}
