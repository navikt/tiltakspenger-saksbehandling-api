package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import org.junit.jupiter.api.Test

class OppdaterMeldekortbehandlingRouteTest {

    @Test
    fun `meldekortperioden kan ikke være frem i tid`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                    tac = tac,
                    vedtaksperiode = 1.januar(2030) til 31.januar(2030),
                    forventetStatus = HttpStatusCode.BadRequest,
                    forventetJsonBody = """{"melding":"Kan ikke sende inn et meldekort før meldekortperioden har begynt.","kode":"meldekortperioden_kan_ikke_være_frem_i_tid"}""",
                )
            }
        }
    }

    @Test
    fun `kan oppdatere meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
            )!!
            val meldekortbehandlingJson = json.getJSONArray("meldekortbehandlinger").getJSONObject(0)
            meldekortbehandlingJson.toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
                  "begrunnelse": null,
                  "avbrutt": null,
                  "attesteringer": [],
                  "saksbehandler": "Z12345",
                  "opprettet": "2025-05-01T01:02:26.456789",
                  "kanIkkeIverksetteUtbetaling": null,
                  "tilbakekrevingId": null,
                  "type": "FØRSTE_BEHANDLING",
                  "meldeperiodeId": "${meldekortbehandling.meldeperiodeLegacy.id}",
                  "beregning": {
                    "beregningstidspunkt": "TIMESTAMP",
                    "beregningForMeldekortetsPeriode": {
                      "beløp": {
                        "totalt": 2682,
                        "barnetillegg": 0,
                        "ordinært": 2682
                      },
                      "kjedeId": "2025-03-31/2025-04-13",
                      "dager": [
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "YTELSEN_FALLER_BORT",
                          "beregningsdag": null,
                          "dato": "2025-03-31",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-01",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-02",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-03",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-04",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-05",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "YTELSEN_FALLER_BORT",
                          "beregningsdag": {
                            "prosent": 0,
                            "beløp": 0,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-06",
                          "status": "IKKE_TILTAKSDAG"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-07",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-08",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-09",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "INGEN_REDUKSJON",
                          "beregningsdag": {
                            "prosent": 100,
                            "beløp": 298,
                            "barnetillegg": 0
                          },
                          "dato": "2025-04-10",
                          "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "YTELSEN_FALLER_BORT",
                          "beregningsdag": null,
                          "dato": "2025-04-11",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "YTELSEN_FALLER_BORT",
                          "beregningsdag": null,
                          "dato": "2025-04-12",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        },
                        {
                          "reduksjonAvYtelsePåGrunnAvFravær": "YTELSEN_FALLER_BORT",
                          "beregningsdag": null,
                          "dato": "2025-04-13",
                          "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                        }
                      ],
                      "periode": {
                        "fraOgMed": "2025-03-31",
                        "tilOgMed": "2025-04-13"
                      }
                    },
                    "totalBeløp": {
                      "totalt": 2682,
                      "barnetillegg": 0,
                      "ordinært": 2682
                    },
                    "beregningerForPåfølgendePerioder": []
                  },
                  "beslutter": null,
                  "simulertBeregning": {
                    "behandlingstype": "MELDEKORT",
                    "simuleringstidspunkt": "2025-01-01T01:02:03.456789",
                    "simulerteBeløp": {
                      "etterbetaling": 2682,
                      "totalJustering": 0,
                      "nyUtbetaling": 2682,
                      "feilutbetaling": 0,
                      "tidligereUtbetaling": 0,
                      "totalTrekk": 0
                    },
                    "simuleringsdato": "2025-05-01",
                    "behandlingId": "${meldekortbehandling.id}",
                    "simuleringTotalBeløp": 2682,
                    "simuleringResultat": "ENDRING",
                    "meldeperioder": [
                      {
                        "simulerteBeløp": {
                          "etterbetaling": 2682,
                          "totalJustering": 0,
                          "nyUtbetaling": 2682,
                          "feilutbetaling": 0,
                          "tidligereUtbetaling": 0,
                          "totalTrekk": 0
                        },
                        "kjedeId": "2025-03-31/2025-04-13",
                        "dager": [
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 0,
                              "totalJustering": 0,
                              "nyUtbetaling": 0,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-03-31",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 0,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 0
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 0
                              }
                            },
                            "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-01",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-02",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-03",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-04",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-05",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 0,
                              "totalJustering": 0,
                              "nyUtbetaling": 0,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-06",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 0,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 0
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 0
                              }
                            },
                            "status": "IKKE_TILTAKSDAG"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-07",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-08",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-09",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 298,
                              "totalJustering": 0,
                              "nyUtbetaling": 298,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-10",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 298,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 298
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 298
                              }
                            },
                            "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 0,
                              "totalJustering": 0,
                              "nyUtbetaling": 0,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-11",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 0,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 0
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 0
                              }
                            },
                            "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 0,
                              "totalJustering": 0,
                              "nyUtbetaling": 0,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-12",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 0,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 0
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 0
                              }
                            },
                            "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                          },
                          {
                            "simulerteBeløp": {
                              "etterbetaling": 0,
                              "totalJustering": 0,
                              "nyUtbetaling": 0,
                              "feilutbetaling": 0,
                              "tidligereUtbetaling": 0,
                              "totalTrekk": 0
                            },
                            "dato": "2025-04-13",
                            "posteringer": [
                              {
                                "klassekode": "test_klassekode",
                                "beløp": 0,
                                "fagområde": "TILTAKSPENGER",
                                "type": "YTELSE"
                              }
                            ],
                            "beregning": {
                              "totalt": {
                                "før": null,
                                "nå": 0
                              },
                              "barnetillegg": {
                                "før": null,
                                "nå": 0
                              },
                              "ordinært": {
                                "før": null,
                                "nå": 0
                              }
                            },
                            "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                          }
                        ],
                        "beregning": {
                          "totalt": {
                            "før": null,
                            "nå": 2682
                          },
                          "barnetillegg": {
                            "før": null,
                            "nå": 0
                          },
                          "ordinært": {
                            "før": null,
                            "nå": 2682
                          }
                        }
                      }
                    ],
                    "beregningstidspunkt": "TIMESTAMP",
                    "beregning": {
                      "totalt": {
                        "før": null,
                        "nå": 2682
                      },
                      "barnetillegg": {
                        "før": null,
                        "nå": 0
                      },
                      "ordinært": {
                        "før": null,
                        "nå": 2682
                      }
                    }
                  },
                  "brukersMeldekortId": "null",
                  "navkontor": "0220",
                  "periode": {
                    "fraOgMed": "2025-03-31",
                    "tilOgMed": "2025-04-13"
                  },
                  "erAvsluttet": false,
                  "navkontorNavn": "Nav Asker",
                  "dager": [
                    {
                      "dato": "2025-03-31",
                      "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                    },
                    {
                      "dato": "2025-04-01",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-02",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-03",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-04",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-05",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-06",
                      "status": "IKKE_TILTAKSDAG"
                    },
                    {
                      "dato": "2025-04-07",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-08",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-09",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-10",
                      "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                    },
                    {
                      "dato": "2025-04-11",
                      "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                    },
                    {
                      "dato": "2025-04-12",
                      "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                    },
                    {
                      "dato": "2025-04-13",
                      "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                    }
                  ],
                  "sakId": "${meldekortbehandling.sakId}",
                  "id": "${meldekortbehandling.id}",
                  "godkjentTidspunkt": null,
                  "utbetalingsstatus": "IKKE_GODKJENT",
                  "tekstTilVedtaksbrev": null,
                  "status": "UNDER_BEHANDLING",
                  "skalSendeVedtaksbrev": true,
                  "harFlereMeldeperioder": false
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan oppdatere meldekortbehandling med legacy body`() {
        withTestApplicationContext { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler")
            val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
            tac.leggTilBruker(jwt, saksbehandler)

            val (sak, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!

            val dagerJson = meldekortbehandling.dagerLegacy.map { dag ->
                val status = when {
                    dag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    dag.status == MeldekortDagStatus.IKKE_BESVART && dag.dato.erHelg() -> MeldekortDagStatus.IKKE_TILTAKSDAG
                    dag.status == MeldekortDagStatus.IKKE_BESVART -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                    else -> dag.status
                }
                dag.dato to status
            }
                .joinToString(prefix = "[", postfix = "]", separator = ",") { (dato, status) ->
                    """
                        {
                            "dato":"$dato",
                            "status":"$status"
                        }
                    """.trimIndent()
                }

            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("/sak/${sak.id}/meldekort/${meldekortbehandling.id}/oppdater")
                },
                jwt = jwt,
            ) {
                setBody(
                    """
                        {
                        "versjon":1,
                        "begrunnelse":null,
                        "tekstTilVedtaksbrev":null,
                        "dager":$dagerJson,
                        "skalSendeVedtaksbrev":true
                        }
                    """.trimIndent(),
                )
            }.apply {
                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
            }

            val oppdatertMeldekortbehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(meldekortbehandling.id)!!
            oppdatertMeldekortbehandling.status shouldBe meldekortbehandling.status
        }
    }

    @Test
    fun `kan velge å ikke sende vedtaksbrev`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling, json) = this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                skalSendeVedtaksbrev = false,
            )!!

            meldekortbehandling.skalSendeVedtaksbrev shouldBe false
            val skalSendeVedtaksbrevJson =
                json.getJSONArray("meldekortbehandlinger").getJSONObject(0).get("skalSendeVedtaksbrev")
            skalSendeVedtaksbrevJson shouldBe false

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)!!.skalSendeVedtaksbrev shouldBe false
        }
    }
}
