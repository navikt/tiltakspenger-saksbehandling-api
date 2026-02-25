package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

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
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptaRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GjenopptaKlagebehandlingMedRammebehandlingRouteTest {

    @Test
    fun `kan gjenoppta klagebehandling med rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedRammebehandling(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = sak.rammevedtaksliste.first().id.toString(),
                status = "UNDER_BEHANDLING",
                kanIverksetteVedtak = null,
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                rammebehandlingId = rammebehandlingMedKlagebehandling.id.toString(),
                ventestatus = """{"sattPåVentAv": "saksbehandlerKlagebehandling","tidspunkt": "TIMESTAMP","begrunnelse": "","erSattPåVent": false,"frist": null}""",
            )
            klagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe "saksbehandlerKlagebehandling"

            rammebehandlingMedKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = LocalDate.parse("2025-01-14"),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `kan gjenoppta rammebehandling tilknyttet klage`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val (_, _, oppdatertRammebehandlingMedKlagebehandling, json) = gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val klagebehandling = oppdatertRammebehandlingMedKlagebehandling.klagebehandling!!

            json.toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
  "id": "${oppdatertRammebehandlingMedKlagebehandling.id}",
  "status": "UNDER_BEHANDLING",
  "sakId": "${sak.id}",
  "saksnummer": "${sak.saksnummer}",
  "rammevedtakId": null,
  "saksbehandler": "saksbehandlerKlagebehandling",
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
    "oppslagstidspunkt": "TIMESTAMP"
  },
  "attesteringer": [],
  "vedtaksperiode": null,
  "fritekstTilVedtaksbrev": null,
  "begrunnelseVilkårsvurdering": null,
  "avbrutt": null,
  "sistEndret": "TIMESTAMP",
  "iverksattTidspunkt": null,
  "ventestatus": {
    "sattPåVentAv": "saksbehandlerKlagebehandling",
    "tidspunkt": "TIMESTAMP",
    "begrunnelse": "",
    "erSattPåVent": false,
    "frist": null
  },
  "utbetaling": null,
  "utbetalingskontroll": null,
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
    "opprettet": "TIMESTAMP",
    "tidsstempelHosOss": "TIMESTAMP",
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
            klagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            klagebehandling.saksbehandler shouldBe "saksbehandlerKlagebehandling"
            klagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }
}
