package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettKlagebehandlingMedRammebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med rammebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klageBehandlinger").first().toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:36.456789",
                  "sistEndret": "2025-01-01T01:02:46.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": null,
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                  "status": "KLAR_TIL_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "${sak.rammevedtaksliste.first().id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": "PROSESSUELL_FEIL",
                  "begrunnelse": "Begrunnelse for omgjøring",
                  "rammebehandlingId": "${rammebehandlingMedKlagebehandling.id}",
                  "ventestatus": {
                    "sattPåVentAv": "saksbehandlerKlagebehandling",
                    "tidspunkt": "2025-01-01T01:02:46.456789",
                    "begrunnelse": "begrunnelse for å sette klage på vent",
                    "erSattPåVent": true,
                    "frist": "2025-01-14"
                  },
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null
                }
                """.trimIndent(),
            )
            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe null
            rammebehandlingMedKlagebehandling.ventestatus shouldBe Ventestatus(
                listOf(
                    VentestatusHendelse(
                        tidspunkt = LocalDateTime.parse("2025-01-01T01:02:45.456789"),
                        endretAv = "saksbehandlerKlagebehandling",
                        begrunnelse = "begrunnelse for å sette klage på vent",
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                        frist = LocalDate.parse("2025-01-14"),
                    ),
                ),
            )
            rammebehandlingMedKlagebehandling.sistEndret shouldBe LocalDateTime.parse("2025-01-01T01:02:47.456789")
        }
    }

    @Test
    fun `kan sette rammebehandling tilknyttet klage på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val (_, _, oppdatertRammebehandlingMedKlagebehandling, json) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
                frist = null,
            )!!
            val klagebehandling = oppdatertRammebehandlingMedKlagebehandling.klagebehandling!!
            json.toString().shouldEqualJson(
                """
                {
  "id": "${oppdatertRammebehandlingMedKlagebehandling.id}",
  "status": "KLAR_TIL_BEHANDLING",
  "sakId": "${sak.id}",
  "saksnummer": "${sak.saksnummer}",
  "rammevedtakId": null,
  "saksbehandler": null,
  "beslutter": null,
  "saksopplysninger": {
    "fødselsdato": "2001-01-01",
    "tiltaksdeltagelse": [
      {
        "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
        "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
        "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
        "typeKode": "GRUPPE_AMO",
        "deltagelseFraOgMed": "2023-01-01",
        "deltagelseTilOgMed": "2023-03-31",
        "deltakelseStatus": "Deltar",
        "deltakelseProsent": 100.0,
        "antallDagerPerUke": 5.0,
        "kilde": "Komet",
        "gjennomforingsprosent": null,
        "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0"
      }
    ],
    "periode": {
      "fraOgMed": "2023-01-01",
      "tilOgMed": "2023-03-31"
    },
    "ytelser": [],
    "tiltakspengevedtakFraArena": [],
    "oppslagstidspunkt": "2025-01-01T01:02:39.456789"
  },
  "attesteringer": [],
  "vedtaksperiode": null,
  "fritekstTilVedtaksbrev": null,
  "begrunnelseVilkårsvurdering": null,
  "avbrutt": null,
  "sistEndret": "2025-01-01T01:02:47.456789",
  "iverksattTidspunkt": null,
  "ventestatus": {
    "sattPåVentAv": "saksbehandlerKlagebehandling",
    "tidspunkt": "2025-01-01T01:02:45.456789",
    "begrunnelse": "Begrunnelse for å sette rammebehandling på vent",
    "erSattPåVent": true,
    "frist": null
  },
  "utbetaling": null,
  "klagebehandlingId": "${klagebehandling.id}",
  "resultat": "IKKE_VALGT",
  "søknad": {
    "id": "${(rammebehandlingMedKlagebehandling as Søknadsbehandling).søknad.id}",
    "journalpostId": "123456789",
    "tiltak": {
      "id": "61328250-7d5d-4961-b70e-5cb727a34371",
      "fraOgMed": "2023-01-01",
      "tilOgMed": "2023-03-31",
      "typeKode": "GRUPPEAMO",
      "typeNavn": "Arbeidsmarkedsoppfølging gruppe"
    },
    "tiltaksdeltakelseperiodeDetErSøktOm": {
      "fraOgMed": "2023-01-01",
      "tilOgMed": "2023-03-31"
    },
    "manueltSattTiltak": null,
    "søknadstype": "DIGITAL",
    "barnetillegg": [],
    "opprettet": "2023-01-01T00:00:00",
    "tidsstempelHosOss": "2023-01-01T00:00:00",
    "antallVedlegg": 0,
    "avbrutt": null,
    "kanInnvilges": true,
    "svar": {
      "harSøktPåTiltak": {
        "svar": "JA"
      },
      "harSøktOmBarnetillegg": {
        "svar": "NEI"
      },
      "kvp": {
        "svar": "NEI",
        "periode": null
      },
      "intro": {
        "svar": "NEI",
        "periode": null
      },
      "institusjon": {
        "svar": "NEI",
        "periode": null
      },
      "etterlønn": {
        "svar": "NEI"
      },
      "gjenlevendepensjon": {
        "svar": "NEI",
        "periode": null
      },
      "alderspensjon": {
        "svar": "NEI",
        "fraOgMed": null
      },
      "sykepenger": {
        "svar": "NEI",
        "periode": null
      },
      "supplerendeStønadAlder": {
        "svar": "NEI",
        "periode": null
      },
      "supplerendeStønadFlyktning": {
        "svar": "NEI",
        "periode": null
      },
      "jobbsjansen": {
        "svar": "NEI",
        "periode": null
      },
      "trygdOgPensjon": {
        "svar": "NEI",
        "periode": null
      }
    },
    "behandlingsarsak": null
  },
  "automatiskSaksbehandlet": false,
  "manueltBehandlesGrunner": [],
  "kanInnvilges": true,
  "type": "SØKNADSBEHANDLING"
                }
                """.trimIndent(),
            )
            klagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            klagebehandling.saksbehandler shouldBe null
            klagebehandling.ventestatus shouldBe Ventestatus(
                listOf(
                    VentestatusHendelse(
                        tidspunkt = LocalDateTime.parse("2025-01-01T01:02:46.456789"),
                        endretAv = "saksbehandlerKlagebehandling",
                        begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                        frist = null,
                    ),
                ),
            )
            klagebehandling.sistEndret shouldBe LocalDateTime.parse("2025-01-01T01:02:46.456789")
        }
    }
}
