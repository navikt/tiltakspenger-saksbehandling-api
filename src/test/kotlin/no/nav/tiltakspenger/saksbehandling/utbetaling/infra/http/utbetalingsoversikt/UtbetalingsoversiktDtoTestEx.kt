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
}
