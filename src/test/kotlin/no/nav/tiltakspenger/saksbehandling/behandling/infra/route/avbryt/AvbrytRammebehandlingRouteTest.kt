package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.avbryt

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgFerdigstillOppretholdtKlagebehandlingForSak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

class AvbrytRammebehandlingRouteTest {
    @Test
    fun `oppretter søknadsbehandling og deretter avbryter`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, søknadsbehandling, json) = opprettSøknadsbehandlingOgAvbryt(
                tac = tac,
            )!!
            json.get("søknader").single().toString().shouldEqualJson(
                """
{
  "id": "${søknad.id}",
  "journalpostId": "123456789",
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
  "avbrutt": {
    "avbruttAv": "Z12345",
    "avbruttTidspunkt": "2025-05-01T01:02:13.456789",
    "begrunnelse": "begrunnelse for avbryt søknad og/eller rammebehandling"
  },
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
}
                """.trimIndent(),
            )
            json.get("behandlinger").single().toString().shouldEqualJson(
                """
{
  "id": "${søknadsbehandling!!.id}",
  "status": "AVBRUTT",
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
    "oppslagstidspunkt": "2025-05-01T01:02:06.456789"
  },
  "attesteringer": [],
  "vedtaksperiode": null,
  "fritekstTilVedtaksbrev": null,
  "begrunnelseVilkårsvurdering": null,
  "avbrutt": {
    "avbruttAv": "Z12345",
    "avbruttTidspunkt": "2025-05-01T01:02:13.456789",
    "begrunnelse": "begrunnelse for avbryt søknad og/eller rammebehandling"
  },
  "sistEndret": "2025-05-01T01:02:13.456789",
  "iverksattTidspunkt": null,
  "ventestatus": null,
  "utbetaling": null,
  "utbetalingskontroll": null,
  "resultat": "IKKE_VALGT",
  "søknad": {
    "id": "${søknad.id}",
    "journalpostId": "123456789",
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
    "avbrutt": {
      "avbruttAv": "Z12345",
      "avbruttTidspunkt": "2025-05-01T01:02:13.456789",
      "begrunnelse": "begrunnelse for avbryt søknad og/eller rammebehandling"
    },
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

    /**
     * Se [Rammebehandlinger.oppdaterRammebehandling] for mer context.
     */
    @Test
    fun `kan avbryte rammebehandling hvor klagebehandlingen har flere rammebehandlinger tilknyttet hvor det finnes flere ulike klager og behandlinger`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, omgjøringsbehandling) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!
            val saksbehandler = ObjectMother.saksbehandler(omgjøringsbehandling.saksbehandler!!)
            val (_, rammevedtak) = iverksettOmgjøringInnvilgelseForBehandlingId(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = (sak.vedtaksliste.alle.first() as Rammevedtak).id,
                behandlingId = omgjøringsbehandling.id,
                saksbehandler = saksbehandler,
            )

            val (_, ferdigstiltOpprettholdtKlagebehandling, _) = opprettOgFerdigstillOppretholdtKlagebehandlingForSak(
                tac = tac,
                sak = sak,
                vedtakDetKlagesPå = rammevedtak.id,
            )!!

            val (_, opprettetRammebehandling) = opprettRammebehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = ferdigstiltOpprettholdtKlagebehandling.id,
                vedtakIdSomOmgjøres = rammevedtak.id.toString(),
                type = "REVURDERING_OMGJØRING",
                saksbehandler = saksbehandler,
            )!!

            val (_, _, avbruttRammebehandling, sakJson) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = opprettetRammebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val rammebehandlingJson = sakJson.get("behandlinger").last().toString()
            val klagebehandlingJson = sakJson.get("klageBehandlinger").last().toString()

            rammebehandlingJson.shouldEqualJsonIgnoringTimestamps(
                """{
                              "id": "${avbruttRammebehandling!!.id}",
                              "status": "AVBRUTT",
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
                              "avbrutt": {
                                "avbruttAv": "saksbehandlerKlagebehandling",
                                "avbruttTidspunkt": "TIMESTAMP",
                                "begrunnelse": "begrunnelse for avbryt søknad og/eller rammebehandling"
                              },
                              "sistEndret": "TIMESTAMP",
                              "iverksattTidspunkt": null,
                              "ventestatus": null,
                              "utbetaling": null,
                              "utbetalingskontroll": null,
                              "klagebehandlingId": "${ferdigstiltOpprettholdtKlagebehandling.id}",
                              "tilbakekrevingId": null,
                              "omgjørVedtak": "${rammevedtak.id}",
                              "resultat": "OMGJØRING_IKKE_VALGT",
                              "automatiskOpprettetGrunn": null,
                              "skalSendeVedtaksbrev": true,
                              "type": "REVURDERING"
                            }
                """.trimIndent(),
            )

            klagebehandlingJson.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO(
                resultat = ferdigstiltOpprettholdtKlagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = ferdigstiltOpprettholdtKlagebehandling.id,
                fnr = "12345678911",
                vedtakDetKlagesPå = "${rammevedtak.id}",
                behandlingDetKlagesPå = "${rammevedtak.behandlingId}",
                rammebehandlingId = emptyList(),
                åpenRammebehandlingId = null,
                brevtekst = listOf("""{"tittel":"Avvisning av klage","tekst":"Din klage er dessverre avvist."}"""),
                journalpostIdInnstillingsbrev = ferdigstiltOpprettholdtKlagebehandling.journalpostIdInnstillingsbrev!!.toString(),
                dokumentInfoIder = ferdigstiltOpprettholdtKlagebehandling.dokumentInfoIder.map { it.toString() },
            )
        }
    }
}
