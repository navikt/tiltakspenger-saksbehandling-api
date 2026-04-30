package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgMottaOppretholdtKlagebehandlingFraKa
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
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
                behandlingDetKlagesPå = "${rammevedtakSøknadsbehandling.behandlingId}",
                //language=json
                ventestatus = listOf("""{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "UNDER_BEHANDLING","tidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}"""),
            )
        }
    }

    @Test
    fun `kan sette klagebehandling med status MOTTATT_FRA_KLAGEINSTANS på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling) = opprettSakOgMottaOppretholdtKlagebehandlingFraKa(
                tac = tac,
            )!!

            val (_, klagebehandlingPåVent, sakJson) = settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
            )!!

            val resultat = klagebehandlingPåVent.resultat as Klagebehandlingsresultat.Opprettholdt
            sakJson.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = null,
                status = "KLAR_TIL_BEHANDLING",
                vedtakDetKlagesPå = "${klagebehandlingPåVent.formkrav.vedtakDetKlagesPå!!}",
                behandlingDetKlagesPå = "${klagebehandlingPåVent.formkrav.behandlingDetKlagesPå!!}",
                kanIverksetteVedtak = null,
                resultat = "OPPRETTHOLDT",
                brevtekst = listOf(
                    """{"tittel": "Hva klagesaken gjelder","tekst": "Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"}""",
                    """{"tittel": "Klagers anførsler","tekst": "<saksbehandler fyller ut>"}""",
                    """{"tittel": "Vurdering av klagen","tekst": "<saksbehandler fyller ut>"}""",
                ),
                hjemler = listOf("ARBEIDSMARKEDSLOVEN_17"),
                iverksattOpprettholdelseTidspunkt = true,
                journalføringstidspunktInnstillingsbrev = true,
                distribusjonstidspunktInnstillingsbrev = true,
                oversendtKlageinstansenTidspunkt = true,
                klageinstanshendelser = listOf(
                    """
                     {
                      "klagehendelseId": "${resultat.klageinstanshendelser.single().klagehendelseId}",
                      "klagebehandlingId": "${klagebehandling.id}",
                      "opprettet": "TIMESTAMP",
                      "sistEndret": "TIMESTAMP",
                      "eksternKlagehendelseId": "${resultat.klageinstanshendelser.single().eksternKlagehendelseId}",
                      "avsluttetTidspunkt": "TIMESTAMP",
                      "journalpostreferanser": [],
                      "utfall": "STADFESTELSE",
                      "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
                    }
                    """.trimIndent(),
                ),
                journalpostIdInnstillingsbrev = klagebehandling.journalpostIdInnstillingsbrev!!.toString(),
                dokumentInfoIder = klagebehandling.dokumentInfoIder.map { it.toString() },
                //language=json
                ventestatus = listOf("""{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "MOTTATT_FRA_KLAGEINSTANS","tidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}"""),
            )
        }
    }
}
