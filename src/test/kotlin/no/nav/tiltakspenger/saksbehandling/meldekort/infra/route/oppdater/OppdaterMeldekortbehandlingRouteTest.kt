package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgOppdaterMeldekortbehandling
import org.junit.jupiter.api.Test

class OppdaterMeldekortbehandlingRouteTest {

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
                  "harFlereMeldeperioder": false,
                  "ventestatus": []
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
    fun `meldekortperioden kan ikke være frem i tid`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                vedtaksperiode = 1.januar(2030) til 31.januar(2030),
                forventetStatus = HttpStatusCode.BadRequest,
                medJsonBody = {
                    it harKode "meldekortperioden_kan_ikke_være_frem_i_tid"
                },
            )
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

    @Test
    fun `kan oppdatere meldekortbehandling som spenner over to meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // Starter på en onsdag
            val vedtaksperiode = 1.april(2026) til 30.april(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            val toMeldeperioder = sak.meldeperiodeKjeder.take(2).map {
                it.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            }

            val (oppdatertSak, oppdatertMeldekortbehandling) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = toMeldeperioder,
            )!!

            oppdatertMeldekortbehandling.meldeperioder.meldeperioder.size shouldBe 2
            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.beløpTotal!! shouldBeGreaterThan 0
            oppdatertMeldekortbehandling.periode shouldBe (30.mars(2026) til 26.april(2026))

            oppdatertSak.meldekortbehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `kan oppdatere meldekortbehandling som spenner over tre meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // 30.mars(2026) er en mandag, 10.mai(2026) er en søndag => 3 meldeperioder (14 + 14 + 14 dager)
            val vedtaksperiode = 1.april(2026) til 10.mai(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3

            val treMeldeperioder = sak.meldeperiodeKjeder.take(3).map {
                it.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            }

            val (oppdatertSak, oppdatertMeldekortbehandling) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = treMeldeperioder,
            )!!

            oppdatertMeldekortbehandling.meldeperioder.meldeperioder.size shouldBe 3
            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.beløpTotal!! shouldBeGreaterThan 0
            oppdatertMeldekortbehandling.periode shouldBe (30.mars(2026) til 10.mai(2026))

            oppdatertSak.meldekortbehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med ikke-sammenhengende meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            val vedtaksperiode = 1.april(2026) til 10.mai(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3

            // Hopper over meldeperiode 2 (midten) - kjede 0 og kjede 2 er ikke sammenhengende
            val ikkeSammenhengendeMeldeperioder = listOf(
                sak.meldeperiodeKjeder[0].hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO(),
                sak.meldeperiodeKjeder[2].hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO(),
            )

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = ikkeSammenhengendeMeldeperioder,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med samme meldeperiodekjede flere ganger`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            val vedtaksperiode = 1.april(2026) til 30.april(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            val sammeKjede = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = listOf(sammeKjede, sammeKjede),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med utdatert versjon av meldeperiode`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // Søknadsbehandling dekker tirsdag 31.mars - søndag 12.april (1.april er onsdag)
            // Revurderingen forlenger bakover til mandag 30.mars, slik at 30.mars og 31.mars
            // går fra "ingen rett" til "har rett" og kjeden får en ny versjon.
            val (sak) = this.iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(1.april(2026) til 12.april(2026)),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(30.mars(2026) til 12.april(2026)),
            )

            val kjede = sak.meldeperiodeKjeder.first()
            kjede.size shouldBe 2 // versjon 1 (søknadsbehandling) og versjon 2 (revurdering)

            // Bruker den utdaterte (første) versjonen av meldeperioden. Den har et annet
            // girRett-mønster enn siste versjon, så valideringen i UtfyltMeldeperiode skal feile.
            val utdatertMeldeperiode = kjede.first().tilOppdatertMeldeperiodeDTO()

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
                meldeperioder = listOf(utdatertMeldeperiode),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med kjedeId som ikke finnes på saken`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            val vedtaksperiode = 1.april(2026) til 30.april(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            // Bygger en gyldig DTO basert på sakens første meldeperiode, men setter en kjedeId
            // som ikke finnes på saken.
            val gyldigDto = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            val ukjentKjede = gyldigDto.copy(kjedeId = "2099-01-05/2099-01-18")

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = listOf(ukjentKjede),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }
}
