package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toSimuleringFraHelvedResponse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppsummeringGeneratorTest {

    @Test
    fun `tom liste for oppsummering og detaljer`() {
        //language=json
        val helvedResponse = """
            {
              "oppsummeringer": [],
              "detaljer": {
                "gjelderId": "12345678910",
                "datoBeregnet": "2024-05-12",
                "totalBeløp": 0,
                "perioder": []
              }
            }
        """.trimIndent()
        val meldeperiodeKjeder = MeldeperiodeKjeder(emptyList())
        helvedResponse.toSimuleringFraHelvedResponse(meldeperiodeKjeder, fixedClock) shouldBe Simulering.IngenEndring(
            simuleringstidspunkt = LocalDateTime.now(fixedClock),
        )
    }

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
        val clock = fixedClock
        helvedResponse.toSimuleringFraHelvedResponse(meldeperiodeKjeder, clock) shouldBe Simulering.Endring(
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
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
            simuleringstidspunkt = LocalDateTime.now(fixedClock),
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
            "beløp": 71,
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
            "beløp": 214,
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
        val clock = fixedClock
        helvedResponse.toSimuleringFraHelvedResponse(meldeperiodeKjeder, clock) shouldBe Simulering.Endring(
            totalBeløp = 0,
            datoBeregnet = 16.mai(2025),
            simuleringstidspunkt = LocalDateTime.now(fixedClock),
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 71,
                            harJustering = false,
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
                                        beløp = 71,
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
                                totalTrekk = 0,
                                totalJustering = 0,
                                totalMotpostering = 0,
                                harJustering = false,
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
                                totalTrekk = 0,
                                totalJustering = 0,
                                totalMotpostering = 0,
                                harJustering = false,
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
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 214,
                            harJustering = false,
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
                                        beløp = 214,
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

    @Test
    fun `sammensatt endring av antall barn i dev`() {
        // Rammebehandling med innvilgelse fra 2025-02-05 til 2025-05-04

        // language=json
        val jsonFraHelved: String = """
            {
              "oppsummeringer": [
                {
                  "fom": "2025-03-03",
                  "tom": "2025-03-21",
                  "tidligereUtbetalt": 3080,
                  "nyUtbetaling": 4620,
                  "totalEtterbetaling": 1540,
                  "totalFeilutbetaling": 0
                },
                {
                  "fom": "2025-04-07",
                  "tom": "2025-04-30",
                  "tidligereUtbetalt": 3960,
                  "nyUtbetaling": 5940,
                  "totalEtterbetaling": 1980,
                  "totalFeilutbetaling": 0
                },
                {
                  "fom": "2025-05-01",
                  "tom": "2025-05-02",
                  "tidligereUtbetalt": 440,
                  "nyUtbetaling": 660,
                  "totalEtterbetaling": 220,
                  "totalFeilutbetaling": 0
                }
              ],
              "detaljer": {
                "gjelderId": "19418513449",
                "datoBeregnet": "2025-09-16",
                "totalBeløp": 3740,
                "perioder": [
                  {
                    "fom": "2025-03-03",
                    "tom": "2025-03-06",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-03-03",
                        "tom": "2025-03-06",
                        "beløp": 1320,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-03-03",
                        "tom": "2025-03-06",
                        "beløp": -880,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-03-10",
                    "tom": "2025-03-14",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-03-10",
                        "tom": "2025-03-14",
                        "beløp": 1650,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-03-10",
                        "tom": "2025-03-14",
                        "beløp": -1100,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-03-17",
                    "tom": "2025-03-21",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-03-17",
                        "tom": "2025-03-21",
                        "beløp": 1650,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-03-17",
                        "tom": "2025-03-21",
                        "beløp": -1100,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-04-07",
                    "tom": "2025-04-11",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-07",
                        "tom": "2025-04-11",
                        "beløp": 1650,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-07",
                        "tom": "2025-04-11",
                        "beløp": -1100,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-04-14",
                    "tom": "2025-04-18",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-14",
                        "tom": "2025-04-18",
                        "beløp": 1650,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-14",
                        "tom": "2025-04-18",
                        "beløp": -1100,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-04-21",
                    "tom": "2025-04-25",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-21",
                        "tom": "2025-04-25",
                        "beløp": 1650,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-21",
                        "tom": "2025-04-25",
                        "beløp": -1100,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-04-28",
                    "tom": "2025-04-30",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-28",
                        "tom": "2025-04-30",
                        "beløp": 990,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-04-28",
                        "tom": "2025-04-30",
                        "beløp": -660,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  },
                  {
                    "fom": "2025-05-01",
                    "tom": "2025-05-02",
                    "posteringer": [
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-05-01",
                        "tom": "2025-05-02",
                        "beløp": 660,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      },
                      {
                        "fagområde": "TILTAKSPENGER",
                        "sakId": "202509051009",
                        "fom": "2025-05-01",
                        "tom": "2025-05-02",
                        "beløp": -440,
                        "type": "YTELSE",
                        "klassekode": "TPBTOPPFAGR"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        /**
         * meldeperiode_01K4D0CG8082WB763PCXTPRGDC	2025-02-24/2025-03-09	1	sak_01K4CP67JTCP6TBYJFRTRSHKJW	2025-09-05 15:17:21.536 +0200	2025-02-24	2025-03-09	9	{"2025-02-24": false, "2025-02-25": false, "2025-02-26": false, "2025-02-27": false, "2025-02-28": false, "2025-03-01": true, "2025-03-02": true, "2025-03-03": true, "2025-03-04": true, "2025-03-05": true, "2025-03-06": true, "2025-03-07": true, "2025-03-08": true, "2025-03-09": true}	{"perioderTilVedtakId": [{"periode": {"fraOgMed": "2025-03-01", "tilOgMed": "2025-03-09"}, "vedtakId": "vedtak_01K4D0CG6X51KAG4TXS4YNYDN9"}]}
         * meldeperiode_01K4D0CG80AHPET3JKS7PSZJ6Z	2025-03-10/2025-03-23	1	sak_01K4CP67JTCP6TBYJFRTRSHKJW	2025-09-05 15:17:21.536 +0200	2025-03-10	2025-03-23	10	{"2025-03-10": true, "2025-03-11": true, "2025-03-12": true, "2025-03-13": true, "2025-03-14": true, "2025-03-15": true, "2025-03-16": true, "2025-03-17": true, "2025-03-18": true, "2025-03-19": true, "2025-03-20": true, "2025-03-21": true, "2025-03-22": true, "2025-03-23": true}	{"perioderTilVedtakId": [{"periode": {"fraOgMed": "2025-03-10", "tilOgMed": "2025-03-23"}, "vedtakId": "vedtak_01K4D0CG6X51KAG4TXS4YNYDN9"}]}
         * meldeperiode_01K4D0CG804169TB4VM9X93FZ2	2025-03-24/2025-04-06	1	sak_01K4CP67JTCP6TBYJFRTRSHKJW	2025-09-05 15:17:21.536 +0200	2025-03-24	2025-04-06	10	{"2025-03-24": true, "2025-03-25": true, "2025-03-26": true, "2025-03-27": true, "2025-03-28": true, "2025-03-29": true, "2025-03-30": true, "2025-03-31": true, "2025-04-01": true, "2025-04-02": true, "2025-04-03": true, "2025-04-04": true, "2025-04-05": true, "2025-04-06": true}	{"perioderTilVedtakId": [{"periode": {"fraOgMed": "2025-03-24", "tilOgMed": "2025-04-06"}, "vedtakId": "vedtak_01K4D0CG6X51KAG4TXS4YNYDN9"}]}
         * meldeperiode_01K4D0CG80CW76QRHJF9WTWHAR	2025-04-07/2025-04-20	1	sak_01K4CP67JTCP6TBYJFRTRSHKJW	2025-09-05 15:17:21.536 +0200	2025-04-07	2025-04-20	10	{"2025-04-07": true, "2025-04-08": true, "2025-04-09": true, "2025-04-10": true, "2025-04-11": true, "2025-04-12": true, "2025-04-13": true, "2025-04-14": true, "2025-04-15": true, "2025-04-16": true, "2025-04-17": true, "2025-04-18": true, "2025-04-19": true, "2025-04-20": true}	{"perioderTilVedtakId": [{"periode": {"fraOgMed": "2025-04-07", "tilOgMed": "2025-04-20"}, "vedtakId": "vedtak_01K4D0CG6X51KAG4TXS4YNYDN9"}]}
         * meldeperiode_01K4D0CG80PE0PHKKM49PRM546	2025-04-21/2025-05-04	1	sak_01K4CP67JTCP6TBYJFRTRSHKJW	2025-09-05 15:17:21.536 +0200	2025-04-21	2025-05-04	10	{"2025-04-21": true, "2025-04-22": true, "2025-04-23": true, "2025-04-24": true, "2025-04-25": true, "2025-04-26": true, "2025-04-27": true, "2025-04-28": true, "2025-04-29": true, "2025-04-30": true, "2025-05-01": true, "2025-05-02": true, "2025-05-03": true, "2025-05-04": true}	{"perioderTilVedtakId": [{"periode": {"fraOgMed": "2025-04-21", "tilOgMed": "2025-05-04"}, "vedtakId": "vedtak_01K4D0CG6X51KAG4TXS4YNYDN9"}]}
         */
        val periode1 = 24.februar(2025) til 9.mars(2025)
        val periode2 = 10 til 23.mars(2025)
        val periode3 = 24.mars(2025) til 6.april(2025)
        val periode4 = 7 til 20.april(2025)
        val periode5 = 21.april(2025) til 4.mai(2025)
        val meldeperiodeKjedeId1 = MeldeperiodeKjedeId.fraPeriode(periode1)
        val meldeperiodeKjedeId2 = MeldeperiodeKjedeId.fraPeriode(periode2)
        val meldeperiodeKjedeId3 = MeldeperiodeKjedeId.fraPeriode(periode3)
        val meldeperiodeKjedeId4 = MeldeperiodeKjedeId.fraPeriode(periode4)
        val meldeperiodeKjedeId5 = MeldeperiodeKjedeId.fraPeriode(periode5)
        val saksnummer = Saksnummer("202509051009")
        val fnr = Fnr.fromString("19418513449")
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
        val meldeperiode4 = ObjectMother.meldeperiode(
            periode = periode4,
            kjedeId = meldeperiodeKjedeId4,
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
        )
        val meldeperiode5 = ObjectMother.meldeperiode(
            periode = periode5,
            kjedeId = meldeperiodeKjedeId5,
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
        )
        val meldeperiodeKjeder = MeldeperiodeKjeder(
            listOf(
                MeldeperiodeKjede(meldeperiode1),
                MeldeperiodeKjede(meldeperiode2),
                MeldeperiodeKjede(meldeperiode3),
                MeldeperiodeKjede(meldeperiode4),
                MeldeperiodeKjede(meldeperiode5),
            ),
        )
        val clock = fixedClock
        val actual = jsonFraHelved.toSimuleringFraHelvedResponse(meldeperiodeKjeder, clock) as Simulering.Endring
        actual.totalBeløp shouldBe 3740
        actual.datoBeregnet shouldBe 16.september(2025)
        actual.simuleringPerMeldeperiode.size shouldBe 4
        actual.simuleringPerMeldeperiode[0] shouldBe SimuleringForMeldeperiode(
            meldeperiode = meldeperiode1,
            simuleringsdager = nonEmptyListOf(
                Simuleringsdag(
                    dato = 3.mars(2025),
                    tidligereUtbetalt = 220,
                    nyUtbetaling = 330,
                    totalEtterbetaling = 110,
                    totalFeilutbetaling = 0,
                    totalTrekk = 0,
                    totalJustering = 0,
                    totalMotpostering = 0,
                    harJustering = false,
                    posteringsdag = PosteringerForDag(
                        dato = 3.mars(2025),
                        posteringer = nonEmptyListOf(
                            PosteringForDag(
                                dato = 3.mars(2025),
                                fagområde = "TILTAKSPENGER",
                                beløp = 330,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                            PosteringForDag(
                                dato = 3.mars(2025),
                                fagområde = "TILTAKSPENGER",
                                beløp = -220,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                        ),
                    ),
                ),
            ).plus(
                (4.mars(2025) til 6.mars(2025)).tilDager().map { dato ->
                    Simuleringsdag(
                        dato = dato,
                        tidligereUtbetalt = 220,
                        nyUtbetaling = 330,
                        totalEtterbetaling = 110,
                        totalFeilutbetaling = 0,
                        totalTrekk = 0,
                        totalJustering = 0,
                        totalMotpostering = 0,
                        harJustering = false,
                        posteringsdag = PosteringerForDag(
                            dato = dato,
                            posteringer = nonEmptyListOf(
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = 330,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = -220,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                            ),
                        ),
                    )
                },
            ),
        )
        actual.simuleringPerMeldeperiode[1] shouldBe SimuleringForMeldeperiode(
            meldeperiode = meldeperiode2,
            simuleringsdager = (10.mars(2025) til 14.mars(2025)).tilDager().map { dato ->
                Simuleringsdag(
                    dato = dato,
                    tidligereUtbetalt = 220,
                    nyUtbetaling = 330,
                    totalEtterbetaling = 110,
                    totalFeilutbetaling = 0,
                    totalTrekk = 0,
                    totalJustering = 0,
                    totalMotpostering = 0,
                    harJustering = false,
                    posteringsdag = PosteringerForDag(
                        dato = dato,
                        posteringer = nonEmptyListOf(
                            PosteringForDag(
                                dato = dato,
                                fagområde = "TILTAKSPENGER",
                                beløp = 330,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                            PosteringForDag(
                                dato = dato,
                                fagområde = "TILTAKSPENGER",
                                beløp = -220,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                        ),
                    ),
                )
            }.toNonEmptyListOrNull()!!.plus(
                (17.mars(2025) til 21.mars(2025)).tilDager().map { dato ->
                    Simuleringsdag(
                        dato = dato,
                        tidligereUtbetalt = 220,
                        nyUtbetaling = 330,
                        totalEtterbetaling = 110,
                        totalFeilutbetaling = 0,
                        totalTrekk = 0,
                        totalJustering = 0,
                        totalMotpostering = 0,
                        harJustering = false,
                        posteringsdag = PosteringerForDag(
                            dato = dato,
                            posteringer = nonEmptyListOf(
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = 330,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = -220,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                            ),
                        ),
                    )
                },
            ),
        )
        actual.simuleringPerMeldeperiode[2] shouldBe SimuleringForMeldeperiode(
            meldeperiode = meldeperiode4,
            simuleringsdager = (7.april(2025) til 11.april(2025)).tilDager().map { dato ->
                Simuleringsdag(
                    dato = dato,
                    tidligereUtbetalt = 220,
                    nyUtbetaling = 330,
                    totalEtterbetaling = 110,
                    totalFeilutbetaling = 0,
                    totalTrekk = 0,
                    totalJustering = 0,
                    totalMotpostering = 0,
                    harJustering = false,
                    posteringsdag = PosteringerForDag(
                        dato = dato,
                        posteringer = nonEmptyListOf(
                            PosteringForDag(
                                dato = dato,
                                fagområde = "TILTAKSPENGER",
                                beløp = 330,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                            PosteringForDag(
                                dato = dato,
                                fagområde = "TILTAKSPENGER",
                                beløp = -220,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                        ),
                    ),
                )
            }.toNonEmptyListOrNull()!!.plus(
                (14.april(2025) til 18.april(2025)).tilDager().map { dato ->
                    Simuleringsdag(
                        dato = dato,
                        tidligereUtbetalt = 220,
                        nyUtbetaling = 330,
                        totalEtterbetaling = 110,
                        totalFeilutbetaling = 0,
                        totalTrekk = 0,
                        totalJustering = 0,
                        totalMotpostering = 0,
                        harJustering = false,
                        posteringsdag = PosteringerForDag(
                            dato = dato,
                            posteringer = nonEmptyListOf(
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = 330,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = -220,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                            ),
                        ),
                    )
                },
            ),
        )
        actual.simuleringPerMeldeperiode[3] shouldBe SimuleringForMeldeperiode(
            meldeperiode = meldeperiode5,
            simuleringsdager = (21.april(2025) til 25.april(2025)).tilDager().map { dato ->
                Simuleringsdag(
                    dato = dato,
                    tidligereUtbetalt = 220,
                    nyUtbetaling = 330,
                    totalEtterbetaling = 110,
                    totalFeilutbetaling = 0,
                    totalTrekk = 0,
                    totalJustering = 0,
                    totalMotpostering = 0,
                    harJustering = false,
                    posteringsdag = PosteringerForDag(
                        dato = dato,
                        posteringer = nonEmptyListOf(
                            PosteringForDag(
                                dato = dato,
                                fagområde = "TILTAKSPENGER",
                                beløp = 330,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                            PosteringForDag(
                                dato = dato,
                                fagområde = "TILTAKSPENGER",
                                beløp = -220,
                                type = Posteringstype.YTELSE,
                                klassekode = "TPBTOPPFAGR",
                            ),
                        ),
                    ),
                )
            }.toNonEmptyListOrNull()!!.plus(
                (28.april(2025) til 30.april(2025)).tilDager().map { dato ->
                    Simuleringsdag(
                        dato = dato,
                        tidligereUtbetalt = 220,
                        nyUtbetaling = 330,
                        totalEtterbetaling = 110,
                        totalFeilutbetaling = 0,
                        totalTrekk = 0,
                        totalJustering = 0,
                        totalMotpostering = 0,
                        harJustering = false,
                        posteringsdag = PosteringerForDag(
                            dato = dato,
                            posteringer = nonEmptyListOf(
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = 330,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = -220,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                            ),
                        ),
                    )
                },
            ).plus(
                (1.mai(2025) til 2.mai(2025)).tilDager().map { dato ->
                    Simuleringsdag(
                        dato = dato,
                        tidligereUtbetalt = 220,
                        nyUtbetaling = 330,
                        totalEtterbetaling = 110,
                        totalFeilutbetaling = 0,
                        totalTrekk = 0,
                        totalJustering = 0,
                        totalMotpostering = 0,
                        harJustering = false,
                        posteringsdag = PosteringerForDag(
                            dato = dato,
                            posteringer = nonEmptyListOf(
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = 330,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                                PosteringForDag(
                                    dato = dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = -220,
                                    type = Posteringstype.YTELSE,
                                    klassekode = "TPBTOPPFAGR",
                                ),
                            ),
                        ),
                    )
                },
            ),
        )
    }
}
