package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.Vurderingstype
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgMottaOppretholdtKlagebehandlingFraKa
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
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

    @Test
    fun `kan ikke vurdere klagebehandling som er satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, _, klagebehandling, _) = iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent(
                tac = tac,
            )!!

            vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
                begrunnelse = Begrunnelse.createOrThrow("oppdatert begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.ANNET,
                vurderingstype = Vurderingstype.OMGJØR,
                hjemler = null,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                    {
                      "melding": "Klagebehandlingen er satt på vent. Den må gjenopptas før den kan behandles videre.",
                      "kode": "klagebehandling_er_satt_på_vent"
                    }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan ta klagebehandling som er satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, _, klagebehandling, _) = iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent(
                tac = tac,
            )!!

            taKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandlerSomTar = ObjectMother.saksbehandler("annenSaksbehandler"),
                forventetStatus = HttpStatusCode.OK,
            )
        }
    }

    @Test
    fun `kan legge tilbake klagebehandling som er satt på vent når saksbehandler står på behandlingen`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandling, _) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!

            taKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                saksbehandlerSomTar = saksbehandler,
            )

            leggKlagebehandlingTilbake(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                saksbehandler = saksbehandler,
            )
        }
    }
}
