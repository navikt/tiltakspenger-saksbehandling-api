package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.UtbetalingDtoTestEx

object UtbetalingsoversiktDtoTestEx {
    const val SYNTETISK_ORGANISASJONSNUMMER = "999111222"
    const val SYNTETISK_SAMHANDLERIDENT = "80912345678"

    fun riktSvar(fnr: Fnr): String = UtbetalingDtoTestEx.riktSvar(fnr)

    fun svarUtenValgfrieFelt(fnr: Fnr): String {
        // language=json
        return """
        [
          {
            "posteringsdato": "2025-08-24",
            "utbetaltTil": {
              "aktoertype": "PERSON",
              "ident": "${fnr.verdi}"
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
                "ytelseNettobeloep": 900.1,
                "rettighetshaver": {
                  "aktoertype": "PERSON",
                  "ident": "${fnr.verdi}"
                },
                "skattsum": -99.9,
                "trekksum": -900.75,
                "ytelseskomponentersum": 1900.75
              }
            ]
          }
        ]
        """.trimIndent()
    }

    fun svarMedAktoerIdAlias(fnr: Fnr): String = svarUtenValgfrieFelt(fnr).replace("\"ident\"", "\"aktoerId\"")

    fun svarMedSamhandlerOgOrganisasjon(): String {
        // language=json
        return """
        [
          {
            "posteringsdato": "2025-08-24",
            "utbetaltTil": {
              "aktoertype": "SAMHANDLER",
              "ident": "$SYNTETISK_SAMHANDLERIDENT"
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
                "ytelseNettobeloep": 900.1,
                "rettighetshaver": {
                  "aktoertype": "ORGANISASJON",
                  "ident": "$SYNTETISK_ORGANISASJONSNUMMER"
                },
                "refundertForOrg": {
                  "aktoertype": "ORGANISASJON",
                  "ident": "$SYNTETISK_ORGANISASJONSNUMMER"
                },
                "skattsum": 0,
                "trekksum": 0,
                "ytelseskomponentersum": 900.1
              }
            ]
          }
        ]
        """.trimIndent()
    }

    /**
     * To utbetalinger til [fnr] i august 2025.
     * Den første har fem ytelser: dagpenger innenfor, én uten type, én av typen «Bidragsforskudd», én for mai og én med [annenPerson] som rettighetshaver.
     * Den andre har bare én ytelse uten type.
     */
    fun svarMedYtelserUtenforGrunnlaget(fnr: Fnr, annenPerson: Fnr): String {
        // language=json
        return """
        [
          {
            "posteringsdato": "2025-08-24",
            "utbetaltTil": { "aktoertype": "PERSON", "ident": "${fnr.verdi}", "navn": "Fornavn Etternavn" },
            "utbetaltTilKonto": { "kontonummer": "${UtbetalingDtoTestEx.SYNTETISK_KONTONUMMER}", "kontotype": "Norsk bank" },
            "utbetalingNettobeloep": 5000,
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "Utbetalt",
            "ytelseListe": [
              ${ytelse("Dagpenger", "2025-08-01", "2025-08-14", fnr.verdi)},
              ${ytelse(null, "2025-08-01", "2025-08-14", fnr.verdi)},
              ${ytelse("Bidragsforskudd", "2025-08-01", "2025-08-14", fnr.verdi)},
              ${ytelse("Dagpenger", "2025-05-01", "2025-05-14", fnr.verdi)},
              ${ytelse("Dagpenger", "2025-08-01", "2025-08-14", annenPerson.verdi)}
            ]
          },
          {
            "posteringsdato": "2025-08-25",
            "utbetaltTil": { "aktoertype": "PERSON", "ident": "${fnr.verdi}", "navn": "Fornavn Etternavn" },
            "utbetalingNettobeloep": 100,
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "Utbetalt",
            "ytelseListe": [
              ${ytelse(null, "2025-08-01", "2025-08-14", fnr.verdi)}
            ]
          }
        ]
        """.trimIndent()
    }

    /** Det som blir igjen av [svarMedYtelserUtenforGrunnlaget], slik klienten skriver det. */
    fun avgrensetSvarMedYtelserUtenforGrunnlaget(fnr: Fnr): String {
        // language=json
        return """
        [
          {
            "posteringsdato": "2025-08-24",
            "utbetaltTil": { "aktoertype": "PERSON", "ident": "${fnr.verdi}", "navn": null },
            "utbetaltTilKonto": null,
            "utbetalingNettobeloep": null,
            "utbetalingsmelding": null,
            "utbetalingsdato": null,
            "forfallsdato": null,
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "Utbetalt",
            "ytelseListe": [
              {
                "ytelsestype": "Dagpenger",
                "ytelsesperiode": { "fom": "2025-08-01", "tom": "2025-08-14" },
                "ytelseNettobeloep": 100,
                "rettighetshaver": { "aktoertype": "PERSON", "ident": "${fnr.verdi}", "navn": null },
                "skattsum": 0,
                "trekksum": 0,
                "ytelseskomponentersum": 100,
                "skattListe": [],
                "trekkListe": [],
                "ytelseskomponentListe": [],
                "bilagsnummer": null,
                "refundertForOrg": null
              }
            ]
          }
        ]
        """.trimIndent()
    }

    private fun ytelse(ytelsestype: String?, fom: String, tom: String, rettighetshaver: String): String {
        val type = ytelsestype?.let { "\"$it\"" } ?: "null"
        // language=json
        return """
              {
                "ytelsestype": $type,
                "ytelsesperiode": { "fom": "$fom", "tom": "$tom" },
                "ytelseNettobeloep": 100,
                "rettighetshaver": { "aktoertype": "PERSON", "ident": "$rettighetshaver", "navn": "Fornavn Etternavn" },
                "skattsum": 0,
                "trekksum": 0,
                "ytelseskomponentersum": 100
              }
        """.trimIndent()
    }
}
