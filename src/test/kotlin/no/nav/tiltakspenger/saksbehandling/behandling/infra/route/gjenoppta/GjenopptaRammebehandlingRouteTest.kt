package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenoppta

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import org.junit.jupiter.api.Test

class GjenopptaRammebehandlingRouteTest : GjenopptaRammebehandlingBuilder {
    @Test
    fun `gjenoppta søknadsbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, søknadsbehandling, json) = opprettSøknadsbehandlingOgGjenoppta(tac = tac)!!
            json.toString().shouldEqualJsonIgnoringTimestamps(
                """
{
  "id": "${søknadsbehandling!!.id}",
  "status": "UNDER_BEHANDLING",
  "sakId": "${sak.id}",
  "saksnummer": "202505011001",
  "rammevedtakId": null,
  "klagebehandlingId": null,
  "tilbakekrevingId": null,
  "saksbehandler": "Z12345",
  "beslutter": null,
  "saksopplysninger": {
    "fødselsdato": "2001-01-01",
    "tiltaksdeltagelse": [
      {
        "eksternDeltagelseId": "${søknad.tiltak!!.id}",
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
        "internDeltakelseId": "${søknad.tiltak!!.tiltaksdeltakerId}"
      }
    ],
    "periode": {
      "fraOgMed": "2023-01-01",
      "tilOgMed": "2023-03-31"
    },
    "ytelser": [],
    "tiltakspengevedtakFraArena": [],
    "oppslagstidspunkt": "2025-05-01T01:02:17.456789"
  },
  "attesteringer": [],
  "vedtaksperiode": null,
  "fritekstTilVedtaksbrev": null,
  "begrunnelseVilkårsvurdering": null,
  "avbrutt": null,
  "opprettet": "TIMESTAMP",
  "sistEndret": "2025-05-01T01:02:16.456789",
  "iverksattTidspunkt": null,
  "ventestatus": {
    "sattPåVentAv": "Z12345",
    "tidspunkt": "2025-05-01T01:02:16.456789",
    "begrunnelse": "",
    "erSattPåVent": false,
    "frist": null
  },
  "utbetaling": null,
  "utbetalingskontroll": null,
  "resultat": "IKKE_VALGT",
  "søknad": {
    "id": "${søknad.id}",
    "journalpostId": "${søknad.journalpostId}",
    "tiltak": {
      "id": "${søknad.tiltak!!.id}",
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
  "type": "SØKNADSBEHANDLING",
  "skalSendeVedtaksbrev": true
}
                """.trimIndent(),
            )
        }
    }
}
