package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.UtbetalingDtoTestEx

object UtbetalingsoversiktDtoTestEx {
    const val SYNTETISK_ORGANISASJONSNUMMER = "999111222"
    const val SYNTETISK_SAMHANDLERIDENT = "80912345678"
    const val SYNTETISK_BILAGSNUMMER = "700000001"

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

    /**
     * Svaret fra tjenesten i dev 2026-10-08 for én sak, med [fnr] og et syntetisk bilagsnummer i stedet for de ekte.
     * Én utbetaling postert 28. september 2026 dekker tre måneder tidligere i året, og satsfeltene er 0.
     */
    fun svarMedTreTidligereMånederIEnUtbetaling(fnr: Fnr): String {
        // language=json
        return """
        [
          {
            "ytelseListe": [
              ${ytelseFraDev("2026-01-01", "2026-01-30", "9328.00", listOf("Barnetillegg" to "2464.00", "Tiltakspenger" to "6864.00"), fnr.verdi)},
              ${ytelseFraDev("2026-02-02", "2026-02-27", "8480.00", listOf("Tiltakspenger" to "6240.00", "Barnetillegg" to "2240.00"), fnr.verdi)},
              ${ytelseFraDev("2026-03-02", "2026-03-06", "2120.00", listOf("Tiltakspenger" to "1560.00", "Barnetillegg" to "560.00"), fnr.verdi)}
            ],
            "utbetaltTil": { "aktoertype": "PERSON", "ident": "${fnr.verdi}", "navn": null },
            "utbetalingsmetode": "Norsk bankkonto",
            "utbetalingsstatus": "Utbetalt",
            "posteringsdato": "2026-09-28",
            "forfallsdato": "2026-09-28",
            "utbetalingsdato": "2026-09-28",
            "utbetalingNettobeloep": 19928.00,
            "utbetalingsmelding": "010126 - 060326 Per dag  424,00",
            "utbetaltTilKonto": null
          }
        ]
        """.trimIndent()
    }

    /**
     * Svaret fra tjenesten i dev 2026-10-08 for en annen sak, med [fnr] og et syntetisk bilagsnummer i stedet for de ekte.
     * Ytelsen begynner 5. desember 2025, dagen før sakens behandlingsgrunnlagsperiode, og meldingen dekker ikke hele ytelsesperioden.
     */
    fun svarMedYtelseSomBegynnerFørGrunnlaget(fnr: Fnr): String {
        // language=json
        return """
        [
          {
            "ytelseListe": [
              ${ytelseFraDev("2025-12-05", "2025-12-26", "6854.00", listOf("Tiltakspenger" to "6854.00"), fnr.verdi)}
            ],
            "utbetaltTil": { "aktoertype": "PERSON", "ident": "${fnr.verdi}", "navn": null },
            "utbetalingsmetode": "Norsk bankkonto",
            "utbetalingsstatus": "Utbetalt",
            "posteringsdato": "2026-09-28",
            "forfallsdato": "2026-09-28",
            "utbetalingsdato": "2026-09-28",
            "utbetalingNettobeloep": 6854.00,
            "utbetalingsmelding": "261225 - 261225 Per dag  894,00, 221225 - 251225 Per dag  298,00, 191225 - 191225 Per dag  894,00, 151225 - 181225 Per dag  298,00, 121225 - 121225 Per dag  894,00",
            "utbetaltTilKonto": null
          }
        ]
        """.trimIndent()
    }

    private fun ytelseFraDev(
        fom: String,
        tom: String,
        nettobeløp: String,
        komponenter: List<Pair<String, String>>,
        rettighetshaver: String,
    ): String {
        val komponentliste = komponenter.joinToString(",") { (type, beløp) ->
            """{ "ytelseskomponenttype": "$type", "satsbeloep": 0.00, "satstype": "Dag", "satsantall": 0.0, "ytelseskomponentbeloep": $beløp }"""
        }
        // language=json
        return """
              {
                "ytelsestype": "Tiltakspenger",
                "ytelsesperiode": { "fom": "$fom", "tom": "$tom" },
                "ytelseNettobeloep": $nettobeløp,
                "rettighetshaver": { "aktoertype": "PERSON", "ident": "$rettighetshaver", "navn": null },
                "skattsum": 0.00,
                "trekksum": 0.00,
                "ytelseskomponentersum": $nettobeløp,
                "skattListe": [],
                "trekkListe": [],
                "ytelseskomponentListe": [$komponentliste],
                "bilagsnummer": "$SYNTETISK_BILAGSNUMMER",
                "refundertForOrg": null
              }
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
