package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.personopplysning.Organisasjonsnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Samhandlerident
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Aktør
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Skattetrekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Trekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Ytelseskomponent
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.registrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.registrertYtelse
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Json-en er kontrakten mot rader som alt er lagret, så både skriving og lesing testes mot faste strenger. */
class UtbetalingsoversiktDbJsonTest {
    private val fnr = Fnr.random()

    private val fullUtbetaling = registrertUtbetaling(
        utbetaltTil = Aktør.Samhandler(Samhandlerident("80912345678")),
        forfallsdato = 25.august(2025),
        utbetalingsdato = 26.august(2025),
        nettobeløp = "900.10".toBigDecimal(),
        melding = "melding",
        ytelser = listOf(
            registrertYtelse(
                periode = Periode(1.august(2025), 14.august(2025)),
                rettighetshaver = Aktør.Person(fnr),
                komponenter = listOf(Ytelseskomponent("type", "10.50".toBigDecimal(), "dag", 2.0, "21.00".toBigDecimal())),
                trekk = listOf(Trekk("trekk", "-2.00".toBigDecimal(), "kreditor")),
                skattetrekk = listOf(Skattetrekk("-3.00".toBigDecimal())),
                bilagsnummer = "bilag-1",
                refundertFor = Aktør.Organisasjon(Organisasjonsnummer("999111222")),
            ),
        ),
    )

    //language=json
    private val fullJson = """
        {
          "utbetalinger": [{
            "utbetaltTil": {"type": "SAMHANDLER", "ident": "80912345678"},
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "Utbetalt",
            "posteringsdato": "2025-08-24",
            "forfallsdato": "2025-08-25",
            "utbetalingsdato": "2025-08-26",
            "nettobeløp": 900.10,
            "melding": "melding",
            "ytelser": [{
              "ytelsestype": "Tiltakspenger",
              "periode": {"fraOgMed": "2025-08-01", "tilOgMed": "2025-08-14"},
              "nettobeløp": 900.1,
              "rettighetshaver": {"type": "PERSON", "ident": "${fnr.verdi}"},
              "skattesum": -99.9,
              "trekksum": -900.75,
              "komponentsum": 1900.75,
              "komponenter": [{"type": "type", "satsbeløp": 10.50, "satstype": "dag", "satsantall": 2.0, "beløp": 21.00}],
              "trekk": [{"type": "trekk", "beløp": -2.00, "kreditor": "kreditor"}],
              "skattetrekk": [{"beløp": -3.00}],
              "bilagsnummer": "bilag-1",
              "refundertFor": {"type": "ORGANISASJON", "ident": "999111222"}
            }]
          }]
        }
    """.trimIndent()

    private val minimalUtbetaling = registrertUtbetaling(
        utbetaltTil = Aktør.Person(fnr),
        forfallsdato = null,
        utbetalingsdato = null,
        nettobeløp = null,
        ytelser = listOf(registrertYtelse(ytelsestype = null, rettighetshaver = Aktør.Person(fnr))),
    )

    //language=json
    private val minimalJson = """
        {
          "utbetalinger": [{
            "utbetaltTil": {"type": "PERSON", "ident": "${fnr.verdi}"},
            "utbetalingsmetode": "Til konto",
            "utbetalingsstatus": "Utbetalt",
            "posteringsdato": "2025-08-24",
            "forfallsdato": null,
            "utbetalingsdato": null,
            "nettobeløp": null,
            "melding": null,
            "ytelser": [{
              "ytelsestype": null,
              "periode": {"fraOgMed": "2025-08-01", "tilOgMed": "2025-08-14"},
              "nettobeløp": 900.1,
              "rettighetshaver": {"type": "PERSON", "ident": "${fnr.verdi}"},
              "skattesum": -99.9,
              "trekksum": -900.75,
              "komponentsum": 1900.75,
              "komponenter": [],
              "trekk": [],
              "skattetrekk": [],
              "bilagsnummer": null,
              "refundertFor": null
            }]
          }]
        }
    """.trimIndent()

    @Test
    fun `skriver hele treet med alle aktørtyper`() {
        listOf(fullUtbetaling).toDbJson() shouldEqualJson fullJson
    }

    @Test
    fun `skriver tomme lister og manglende verdier som null`() {
        listOf(minimalUtbetaling).toDbJson() shouldEqualJson minimalJson
    }

    @Test
    fun `leser lagret json`() {
        fullJson.toRegistrerteUtbetalinger() shouldBe listOf(fullUtbetaling)
        minimalJson.toRegistrerteUtbetalinger() shouldBe listOf(minimalUtbetaling)
    }

    @Test
    fun `skriver metadata`() {
        UtbetalingsoversiktMetadata(
            request = "POST http://test",
            response = "[]",
            statusKode = 200,
            correlationId = CorrelationId("korrelasjon-1"),
            requestSendt = 5.august(2025).atTime(12, 0, 0),
            responsMottatt = 5.august(2025).atTime(12, 0, 1, 500_000_000),
            varighet = 1500.milliseconds,
            antallForsøk = 2,
        ).toDbJson() shouldEqualJson """
            {
              "request": "POST http://test",
              "response": "[]",
              "statusKode": 200,
              "correlationId": "korrelasjon-1",
              "requestSendt": "2025-08-05T12:00:00",
              "responsMottatt": "2025-08-05T12:00:01.5",
              "varighetMs": 1500,
              "antallForsøk": 2
            }
        """.trimIndent()

        UtbetalingsoversiktMetadata(
            request = "POST http://test",
            response = null,
            statusKode = null,
            correlationId = CorrelationId("korrelasjon-2"),
            requestSendt = null,
            responsMottatt = null,
            varighet = 20.seconds,
            antallForsøk = 1,
        ).toDbJson() shouldEqualJson """
            {
              "request": "POST http://test",
              "response": null,
              "statusKode": null,
              "correlationId": "korrelasjon-2",
              "requestSendt": null,
              "responsMottatt": null,
              "varighetMs": 20000,
              "antallForsøk": 1
            }
        """.trimIndent()
    }
}
