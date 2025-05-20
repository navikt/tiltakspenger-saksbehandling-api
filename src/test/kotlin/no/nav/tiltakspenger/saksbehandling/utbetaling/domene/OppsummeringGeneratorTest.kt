package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.desember
import no.nav.tiltakspenger.libs.periodisering.mai
import no.nav.tiltakspenger.libs.periodisering.november
import no.nav.tiltakspenger.libs.periodisering.oktober
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toSimuleringFraHelvedResponse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppsummeringGeneratorTest {

    @Test
    fun `enkel YTELSE for en meldeperiode`() {
        // Meldeperiode mandag 14. oktober til søndag 27. oktober 2024
        // language=json
        val helvedResponse = """
            {
              "oppsummeringer": [
                {
                  "fom": "2024-10-14",
                  "tom": "2024-10-25",
                  "tidligereUtbetalt": 0,
                  "nyUtbetaling": 2280,
                  "totalEtterbetaling": 2280,
                  "totalFeilutbetaling": 0
                }
              ],
              "detaljer": {
                "gjelderId": "01487905247",
                "datoBeregnet": "2025-05-12",
                "totalBeløp": 2280,
                "perioder": [
                  {
                    "fom": "2024-10-14",
                    "tom": "2024-10-17",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202501291001",
                        "fom": "2024-10-14",
                        "tom": "2024-10-17",
                        "beløp": 1140,
                        "type": "YTELSE",
                        "klassekode": "TPTPATT"
                      }
                    ]
                  },
                  {
                    "fom": "2024-10-21",
                    "tom": "2024-10-22",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202501291001",
                        "fom": "2024-10-21",
                        "tom": "2024-10-22",
                        "beløp": 570,
                        "type": "YTELSE",
                        "klassekode": "TPTPATT"
                      }
                    ]
                  },
                  {
                    "fom": "2024-10-24",
                    "tom": "2024-10-25",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202501291001",
                        "fom": "2024-10-24",
                        "tom": "2024-10-25",
                        "beløp": 570,
                        "type": "YTELSE",
                        "klassekode": "TPTPATT"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val periode = Periode(LocalDate.parse("2024-10-14"), LocalDate.parse("2024-10-27"))
        val meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(
            periode,
        )
        val meldeperiode = ObjectMother.meldeperiode(
            periode = periode,
            kjedeId = meldeperiodeKjedeId,
            saksnummer = Saksnummer("202501291001"),
            fnr = Fnr.fromString("01487905247"),
        )
        val meldeperiodeKjeder = MeldeperiodeKjeder(
            MeldeperiodeKjede(
                meldeperiode,
            ),
        )
        helvedResponse.toSimuleringFraHelvedResponse(meldeperiodeKjeder) shouldBe Simulering.Endring(
            simuleringPerMeldeperiode = nonEmptyListOf(
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiodeKjeder.hentForMeldeperiodeId(meldeperiode.id)!!,
                    simuleringsdager = nonEmptyListOf(
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-14"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-14"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-14"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-15"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-15"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-15"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-16"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-16"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-16"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-17"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-17"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-17"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-21"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-21"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-21"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-22"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-22"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-22"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-24"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-24"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-24"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                        Simuleringsdag(
                            dato = LocalDate.parse("2024-10-25"),
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 285,
                            totalEtterbetaling = 285,
                            totalFeilutbetaling = 0,
                            posteringsdag = PosteringerForDag(
                                dato = LocalDate.parse("2024-10-25"),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = LocalDate.parse("2024-10-25"),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPATT",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            datoBeregnet = LocalDate.parse("2025-05-12"),
            totalBeløp = 2280,
        )
    }

    @Test
    fun `feilutbetaling `() {
        /**
         * Endret en dag i første meldeperiode fra deltatt uten lønn til syk bruker. Så vi får påfølgende endringer på de på følgende periodene
         *
         * Meldeperiode:
         *   28.10.2024 - 10-11-2024 (endret fra 100% til 100% -> ingen endring)
         * Påvirker også meldeperiodene:
         *   11.11.2024 - 24.11.2024 2211 -> 2140 (71 kroner/100->75% av 285)
         *   25.11.2024 - 08.12.2024 2281 -> 2067 (214 kroner/75%->0% av 285)
         *
         * Dette er en buggy simulering/utbetaling pga. bug ved bruk av kode 7.
         * Fra dev. Deltatt 10 ukedager i første vedtak. Korrigerer 2. og 3. desember til deltatt med lønn (fra 100% ->0%)
         * Forventer minus 570 (2*285) kroner til feilkonto.
         *
         * Vi hadde forventet at det var tidligere utbetalt 1425 kroner i perioden 2. desember til 6. desember. (285*5).
         * Vi hadde forventet 2 dager feilutbetaling (2*285) i perioden 2. desember til 3. desember og tilhørende motpostering.
         */
        //language=json
        val helvedResponse = """
           {
  "oppsummeringer": [
    {
      "fom": "2024-11-11",
      "tom": "2024-11-22",
      "tidligereUtbetalt": 2211,
      "nyUtbetaling": 2140,
      "totalEtterbetaling": 0,
      "totalFeilutbetaling": 71
    },
    {
      "fom": "2024-12-05",
      "tom": "2024-12-05",
      "tidligereUtbetalt": 214,
      "nyUtbetaling": 0,
      "totalEtterbetaling": 0,
      "totalFeilutbetaling": 214
    }
  ],
  "detaljer": {
    "gjelderId": "22469635663",
    "datoBeregnet": "2025-05-16",
    "totalBeløp": 0,
    "perioder": [
      {
        "fom": "2024-11-11",
        "tom": "2024-11-11",
        "posteringer": [
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-11",
            "tom": "2024-11-11",
            "beløp": 71,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-11",
            "tom": "2024-11-11",
            "beløp": 214,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-11",
            "tom": "2024-11-11",
            "beløp": 71,
            "type": "FEILUTBETALING",
            "klassekode": "KL_KODE_FEIL_ARBYT"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-11",
            "tom": "2024-11-11",
            "beløp": -71,
            "type": "MOTPOSTERING",
            "klassekode": "TBMOTOBS"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-11",
            "tom": "2024-11-11",
            "beløp": -285,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          }
        ]
      },
      {
        "fom": "2024-11-12",
        "tom": "2024-11-15",
        "posteringer": [
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-12",
            "tom": "2024-11-15",
            "beløp": 856,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-12",
            "tom": "2024-11-15",
            "beløp": -856,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          }
        ]
      },
      {
        "fom": "2024-11-18",
        "tom": "2024-11-22",
        "posteringer": [
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-18",
            "tom": "2024-11-22",
            "beløp": 1070,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-11-18",
            "tom": "2024-11-22",
            "beløp": -1070,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          }
        ]
      },
      {
        "fom": "2024-12-05",
        "tom": "2024-12-05",
        "posteringer": [
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-12-05",
            "tom": "2024-12-05",
            "beløp": 214,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-12-05",
            "tom": "2024-12-05",
            "beløp": 214,
            "type": "FEILUTBETALING",
            "klassekode": "KL_KODE_FEIL_ARBYT"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-12-05",
            "tom": "2024-12-05",
            "beløp": -214,
            "type": "MOTPOSTERING",
            "klassekode": "TBMOTOBS"
          },
          {
            "fagområde": "TILTAKSPENGER",
            "sakId": "202504101001",
            "fom": "2024-12-05",
            "tom": "2024-12-05",
            "beløp": -214,
            "type": "YTELSE",
            "klassekode": "TPTPGRAMO"
          }
        ]
      }
    ]
  }
}
        """.trimIndent()

        val periode1 = 28.oktober(2024) til 10.november(2024)
        val periode2 = 11 til 24.november(2024)
        val periode3 = 25.november(2024) til 8.desember(2024)
        val meldeperiodeKjedeId1 = MeldeperiodeKjedeId.fraPeriode(periode1)
        val meldeperiodeKjedeId2 = MeldeperiodeKjedeId.fraPeriode(periode2)
        val meldeperiodeKjedeId3 = MeldeperiodeKjedeId.fraPeriode(periode3)
        val saksnummer = Saksnummer("202504101001")
        val fnr = Fnr.fromString("22469635663")
        val sakId = SakId.random()
        val meldeperiode1 = ObjectMother.meldeperiode(
            periode = periode1,
            kjedeId = meldeperiodeKjedeId1,
            fnr = fnr,
            saksnummer = saksnummer,
            sakId = sakId,
        )
        val meldeperiode2 = ObjectMother.meldeperiode(
            periode = periode2,
            kjedeId = meldeperiodeKjedeId2,
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
        )
        val meldeperiode3 = ObjectMother.meldeperiode(
            periode = periode3,
            kjedeId = meldeperiodeKjedeId3,
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
        )
        val meldeperiodeKjeder = MeldeperiodeKjeder(
            listOf(
                MeldeperiodeKjede(meldeperiode1),
                MeldeperiodeKjede(meldeperiode2),
                MeldeperiodeKjede(meldeperiode3),
            ),
        )
        helvedResponse.toSimuleringFraHelvedResponse(meldeperiodeKjeder) shouldBe Simulering.Endring(
            totalBeløp = 0,
            datoBeregnet = 16.mai(2025),
            simuleringPerMeldeperiode = nonEmptyListOf(
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode2,
                    simuleringsdager = nonEmptyListOf(
                        Simuleringsdag(
                            dato = 11.november(2024),
                            tidligereUtbetalt = 285,
                            nyUtbetaling = 214,
                            totalEtterbetaling = 0,
                            totalFeilutbetaling = 71,
                            posteringsdag = PosteringerForDag(
                                dato = 11.november(2024),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = 11.november(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 71,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPGRAMO",
                                    ),
                                    PosteringForDag(
                                        dato = 11.november(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 214,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPGRAMO",
                                    ),
                                    PosteringForDag(
                                        dato = 11.november(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 71,
                                        type = Posteringstype.FEILUTBETALING,
                                        klassekode = "KL_KODE_FEIL_ARBYT",
                                    ),
                                    PosteringForDag(
                                        dato = 11.november(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = -71,
                                        type = Posteringstype.MOTPOSTERING,
                                        klassekode = "TBMOTOBS",
                                    ),
                                    PosteringForDag(
                                        dato = 11.november(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = -285,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPGRAMO",
                                    ),
                                ),
                            ),
                        ),

                    ).plus(
                        (12.november(2024) til 15.november(2024)).tilDager().map { dato ->
                            Simuleringsdag(
                                dato = dato,
                                tidligereUtbetalt = 214,
                                nyUtbetaling = 214,
                                totalEtterbetaling = 0,
                                totalFeilutbetaling = 0,
                                posteringsdag = PosteringerForDag(
                                    dato = dato,
                                    posteringer = nonEmptyListOf(
                                        PosteringForDag(
                                            dato = dato,
                                            fagområde = "TILTAKSPENGER",
                                            beløp = 214,
                                            type = Posteringstype.YTELSE,
                                            klassekode = "TPTPGRAMO",
                                        ),
                                        PosteringForDag(
                                            dato = dato,
                                            fagområde = "TILTAKSPENGER",
                                            beløp = -214,
                                            type = Posteringstype.YTELSE,
                                            klassekode = "TPTPGRAMO",
                                        ),
                                    ),
                                ),
                            )
                        },
                    ).plus(
                        (18.november(2024) til 22.november(2024)).tilDager().map { dato ->
                            Simuleringsdag(
                                dato = dato,
                                tidligereUtbetalt = 214,
                                nyUtbetaling = 214,
                                totalEtterbetaling = 0,
                                totalFeilutbetaling = 0,
                                posteringsdag = PosteringerForDag(
                                    dato = dato,
                                    posteringer = nonEmptyListOf(
                                        PosteringForDag(
                                            dato = dato,
                                            fagområde = "TILTAKSPENGER",
                                            beløp = 214,
                                            type = Posteringstype.YTELSE,
                                            klassekode = "TPTPGRAMO",
                                        ),
                                        PosteringForDag(
                                            dato = dato,
                                            fagområde = "TILTAKSPENGER",
                                            beløp = -214,
                                            type = Posteringstype.YTELSE,
                                            klassekode = "TPTPGRAMO",
                                        ),
                                    ),
                                ),
                            )
                        },
                    ),
                ),
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode3,
                    simuleringsdager = nonEmptyListOf(
                        Simuleringsdag(
                            dato = 5.desember(2024),
                            tidligereUtbetalt = 214,
                            nyUtbetaling = 0,
                            totalEtterbetaling = 0,
                            totalFeilutbetaling = 214,
                            posteringsdag = PosteringerForDag(
                                dato = 5.desember(2024),
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = 5.desember(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 214,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPGRAMO",
                                    ),
                                    PosteringForDag(
                                        dato = 5.desember(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = 214,
                                        type = Posteringstype.FEILUTBETALING,
                                        klassekode = "KL_KODE_FEIL_ARBYT",
                                    ),
                                    PosteringForDag(
                                        dato = 5.desember(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = -214,
                                        type = Posteringstype.MOTPOSTERING,
                                        klassekode = "TBMOTOBS",
                                    ),
                                    PosteringForDag(
                                        dato = 5.desember(2024),
                                        fagområde = "TILTAKSPENGER",
                                        beløp = -214,
                                        type = Posteringstype.YTELSE,
                                        klassekode = "TPTPGRAMO",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
