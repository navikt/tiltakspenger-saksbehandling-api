package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Eksempelsvar fra sokos-utbetaldata, bygget fra den offisielle spec-en og konsumentenes fixturer.
 *
 * [UtbetalingDto] leser i dag bare `ytelseListe[].ytelsestype` og `ytelseListe[].ytelsesperiode`, og resten av svaret droppes stille av Jackson.
 * Fixturen tar med alle nivåene spec-en beskriver — utbetalingsnivå, ytelsesnivå, komponenter, trekk, skatt, `bilagsnummer`, `utbetaltTil` og `utbetaltTilKonto` — nettopp fordi de ikke er lest ennå.
 * Da fanger klienttestene at mappingen fortsatt gir det samme når DTO-en utvides med feltene.
 *
 * Identer sendes inn slik at repoets egen [Fnr]-generator brukes; kontonummeret er syntetisk.
 */
object UtbetalingDtoTestEx {

    /** Syntetisk kontonummer — tredje siffer er 9, slik Folkeregisterets 2032-standard markerer syntetiske verdier. */
    const val SYNTETISK_KONTONUMMER = "12987654321"

    /**
     * Et svar med to utbetalinger: én utbetalt og én kommende.
     *
     * Den kommende har fire ytelser, slik at alle grenene i mappingen dekkes av ett svar:
     * to perioder på samme ytelsestype (tiltakspenger, én i hver utbetaling), en annen kjent type (dagpenger), en type vi ikke kjenner («Økonomisk sosialhjelp») som filtreres bort, og en `null`-type som blir [Ytelsetype.UKJENT].
     */
    fun riktSvar(
        fnr: Fnr,
        kontonummer: String = SYNTETISK_KONTONUMMER,
    ): String {
        // language=json
        return """
        [
          {
            "posteringsdato": "2025-08-24",
            "utbetaltTil": {
              "aktoertype": "PERSON",
              "ident": "${fnr.verdi}",
              "navn": "Fornavn Etternavn"
            },
            "utbetalingNettobeloep": 900.1,
            "utbetalingsmelding": "En eller annen melding",
            "utbetalingsdato": "2025-08-15",
            "forfallsdato": "2025-08-24",
            "utbetaltTilKonto": {
              "kontonummer": "$kontonummer",
              "kontotype": "Norsk bank"
            },
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "Utbetalt",
            "ytelseListe": [
              {
                "ytelsestype": "Tiltakspenger",
                "ytelsesperiode": {
                  "fom": "2025-08-01",
                  "tom": "2025-08-14"
                },
                "ytelseskomponentListe": [
                  {
                    "ytelseskomponenttype": "Tiltakspenger",
                    "satsbeloep": 500.25,
                    "satstype": "Dag",
                    "satsantall": 3,
                    "ytelseskomponentbeloep": 1500.75
                  },
                  {
                    "ytelseskomponenttype": "Barnetillegg",
                    "satstype": null,
                    "ytelseskomponentbeloep": 400
                  }
                ],
                "ytelseskomponentersum": 1900.75,
                "trekkListe": [
                  {
                    "trekktype": "Skatt",
                    "trekkbeloep": -300.25,
                    "kreditor": "Skatteetaten"
                  },
                  {
                    "trekktype": "Annet trekk",
                    "trekkbeloep": -600.50,
                    "kreditor": "Namsmannen"
                  }
                ],
                "trekksum": -900.75,
                "skattListe": [
                  {
                    "skattebeloep": -99.9
                  }
                ],
                "skattsum": -99.9,
                "ytelseNettobeloep": 900.1,
                "bilagsnummer": "84172491",
                "rettighetshaver": {
                  "aktoertype": "PERSON",
                  "ident": "${fnr.verdi}",
                  "navn": "Fornavn Etternavn"
                }
              }
            ]
          },
          {
            "posteringsdato": "2025-09-09",
            "utbetaltTil": {
              "aktoertype": "PERSON",
              "ident": "${fnr.verdi}",
              "navn": "Fornavn Etternavn"
            },
            "utbetalingNettobeloep": 8700,
            "utbetalingsmelding": null,
            "utbetalingsdato": null,
            "forfallsdato": "2025-09-19",
            "utbetaltTilKonto": {
              "kontonummer": "$kontonummer",
              "kontotype": "Norsk bank"
            },
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "something",
            "ytelseListe": [
              {
                "ytelsestype": "Tiltakspenger",
                "ytelsesperiode": {
                  "fom": "2025-09-01",
                  "tom": "2025-09-14"
                },
                "ytelseskomponentListe": [
                  {
                    "ytelseskomponenttype": "Tiltakspenger",
                    "satsbeloep": 285,
                    "satstype": "Dag",
                    "satsantall": 10,
                    "ytelseskomponentbeloep": 2850
                  }
                ],
                "ytelseskomponentersum": 2850,
                "trekkListe": [],
                "trekksum": 0,
                "skattListe": [],
                "skattsum": 0,
                "ytelseNettobeloep": 2850,
                "bilagsnummer": null,
                "rettighetshaver": {
                  "aktoertype": "PERSON",
                  "ident": "${fnr.verdi}",
                  "navn": "Fornavn Etternavn"
                }
              },
              {
                "ytelsestype": "Dagpenger",
                "ytelsesperiode": {
                  "fom": "2025-09-01",
                  "tom": "2025-09-30"
                },
                "ytelseskomponentListe": [
                  {
                    "ytelseskomponenttype": "Dagpenger",
                    "satstype": null,
                    "ytelseskomponentbeloep": 4200
                  }
                ],
                "ytelseskomponentersum": 4200,
                "trekkListe": [],
                "trekksum": 0,
                "skattListe": [],
                "skattsum": 0,
                "ytelseNettobeloep": 4200,
                "bilagsnummer": null,
                "rettighetshaver": {
                  "aktoertype": "PERSON",
                  "ident": "${fnr.verdi}",
                  "navn": "Fornavn Etternavn"
                }
              },
              {
                "ytelsestype": "Økonomisk sosialhjelp",
                "ytelsesperiode": {
                  "fom": "2025-09-01",
                  "tom": "2025-09-30"
                },
                "ytelseskomponentListe": [],
                "ytelseskomponentersum": 1650,
                "trekkListe": [],
                "trekksum": 0,
                "skattListe": [],
                "skattsum": 0,
                "ytelseNettobeloep": 1650,
                "bilagsnummer": null,
                "rettighetshaver": {
                  "aktoertype": "PERSON",
                  "ident": "${fnr.verdi}",
                  "navn": "Fornavn Etternavn"
                }
              },
              {
                "ytelsestype": null,
                "ytelsesperiode": {
                  "fom": "2025-08-18",
                  "tom": "2025-08-31"
                },
                "ytelseskomponentListe": [],
                "ytelseskomponentersum": 0,
                "trekkListe": [],
                "trekksum": 0,
                "skattListe": [],
                "skattsum": 0,
                "ytelseNettobeloep": 0,
                "bilagsnummer": null,
                "rettighetshaver": {
                  "aktoertype": "PERSON",
                  "ident": "${fnr.verdi}",
                  "navn": "Fornavn Etternavn"
                }
              }
            ]
          }
        ]
        """.trimIndent()
    }

    /**
     * Ytelsene [riktSvar] skal gi etter dagens mapping.
     *
     * Rekkefølgen er den `groupBy` gir: ytelsestypen som først dukker opp i den flate lista over alle `ytelseListe`-elementene, kommer først.
     * «Økonomisk sosialhjelp» er filtrert bort før grupperingen fordi den ikke finnes i [Ytelsetype].
     */
    val forventedeYtelserFraRiktSvar: List<Ytelse> = listOf(
        Ytelse(
            ytelsetype = Ytelsetype.TILTAKSPENGER,
            perioder = listOf(
                Periode(1.august(2025), 14.august(2025)),
                Periode(1.september(2025), 14.september(2025)),
            ),
        ),
        Ytelse(
            ytelsetype = Ytelsetype.DAGPENGER,
            perioder = listOf(Periode(1.september(2025), 30.september(2025))),
        ),
        Ytelse(
            ytelsetype = Ytelsetype.UKJENT,
            perioder = listOf(Periode(18.august(2025), 31.august(2025))),
        ),
    )

    /**
     * Svaret slik det ser ut med bare feltene dagens [UtbetalingDto] kjenner.
     *
     * Pinner at mappingen ikke har begynt å kreve noen av de nye feltene.
     */
    fun minimaltSvar(): String {
        // language=json
        return """
        [
          {
            "ytelseListe": [
              {
                "ytelsestype": "Tiltakspenger",
                "ytelsesperiode": {
                  "fom": "2025-08-01",
                  "tom": "2025-08-14"
                }
              }
            ]
          }
        ]
        """.trimIndent()
    }
}
