package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater

import io.kotest.assertions.json.shouldEqualJson
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
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
            val meldekortbehandlingJson = json.getJSONArray("meldekortBehandlinger").getJSONObject(0)
            meldekortbehandlingJson.toString().shouldEqualJson(
                """
                {
                  "begrunnelse": null,
                  "avbrutt": null,
                  "attesteringer": [],
                  "saksbehandler": "Z12345",
                  "opprettet": "2025-05-01T01:02:23.456789",
                  "type": "FØRSTE_BEHANDLING",
                  "meldeperiodeId": "${meldekortbehandling.meldeperiode.id}",
                  "beregning": {
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
                    "simuleringstidspunkt": null,
                    "simulerteBeløp": {
                      "etterbetaling": 2682,
                      "totalJustering": 0,
                      "nyUtbetaling": 2682,
                      "feilutbetaling": 0,
                      "tidligereUtbetaling": 0,
                      "totalTrekk": 0
                    },
                    "simuleringsdato": "2025-01-01",
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
                    "beregningstidspunkt": null,
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
                    },
                    "utbetalingValideringsfeil": null
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
                  "status": "UNDER_BEHANDLING"
                }
                """.trimIndent(),
            )
        }
    }
}
