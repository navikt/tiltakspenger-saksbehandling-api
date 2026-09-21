package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Aktør
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertYtelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Skattetrekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Trekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMappingfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Ytelseskomponent
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/** Klienten kjøres over [FakeHttpTransport], så auth, statusregel, Jackson og metadata er med i testen. */
class UtbetalingsoversiktHttpKlientTest {
    private val baseUrl = "http://utbetaldata.test"
    private val correlationId = CorrelationId.generate()
    private val fnr = Fnr.random()
    private val periode = Periode(fraOgMed = 1.august(2025), tilOgMed = 30.september(2025))

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun klient(
        transport: FakeHttpTransport,
        authTokenProvider: AuthTokenProvider = this.authTokenProvider,
    ) = UtbetalingsoversiktHttpKlient(
        baseUrl = baseUrl,
        clock = ObjectMother.clock,
        authTokenProvider = authTokenProvider,
        transport = transport,
    )

    private fun transportMed(json: String) = FakeHttpTransport().apply { leggIKøJson(json, statusCode = 200) }

    private fun transportMedStatus(statusCode: Int) = FakeHttpTransport().apply {
        leggIKøStatus(statusCode = statusCode, body = "avvist", contentType = "text/plain")
    }

    private suspend fun hent(
        transport: FakeHttpTransport,
        periodetype: Oppslagsperiodetype = Oppslagsperiodetype.YTELSESPERIODE,
        klient: UtbetalingsoversiktHttpKlient = klient(transport),
    ) = klient.hent(fnr = fnr, periode = periode, periodetype = periodetype, correlationId = correlationId)

    @Test
    fun `klienten kan bygges med standardtransporten`() {
        UtbetalingsoversiktHttpKlient(baseUrl = baseUrl, clock = ObjectMother.clock, authTokenProvider = authTokenProvider)
    }

    /** Hele lista sammenlignes, så et felt som kommer til eller faller bort, må vurderes på nytt. */
    @Test
    fun `fullt svar gir utbetalingene med alle felt, uten kontonummer og navn`() {
        val transport = transportMed(UtbetalingsoversiktDtoTestEx.riktSvar(fnr))

        runTest {
            hent(transport).getOrFail().body shouldBe listOf(
                RegistrertUtbetaling(
                    utbetaltTil = Aktør.Person(fnr),
                    utbetalingsmetode = "Til konto",
                    utbetalingsstatus = "Utbetalt",
                    posteringsdato = 24.august(2025),
                    forfallsdato = 24.august(2025),
                    utbetalingsdato = 15.august(2025),
                    nettobeløp = "900.1".toBigDecimal(),
                    melding = "En eller annen melding",
                    ytelser = listOf(
                        RegistrertYtelse(
                            ytelsestype = "Tiltakspenger",
                            periode = Periode(1.august(2025), 14.august(2025)),
                            nettobeløp = "900.1".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "-99.9".toBigDecimal(),
                            trekksum = "-900.75".toBigDecimal(),
                            komponentsum = "1900.75".toBigDecimal(),
                            komponenter = listOf(
                                Ytelseskomponent(
                                    type = "Tiltakspenger",
                                    satsbeløp = "500.25".toBigDecimal(),
                                    satstype = "Dag",
                                    satsantall = 3.0,
                                    beløp = "1500.75".toBigDecimal(),
                                ),
                                Ytelseskomponent(
                                    type = "Barnetillegg",
                                    satsbeløp = null,
                                    satstype = null,
                                    satsantall = null,
                                    beløp = "400".toBigDecimal(),
                                ),
                            ),
                            trekk = listOf(
                                Trekk(type = "Skatt", beløp = "-300.25".toBigDecimal(), kreditor = "Skatteetaten"),
                                Trekk(type = "Annet trekk", beløp = "-600.50".toBigDecimal(), kreditor = "Namsmannen"),
                            ),
                            skattetrekk = listOf(Skattetrekk(beløp = "-99.9".toBigDecimal())),
                            bilagsnummer = "84172491",
                            refundertFor = null,
                        ),
                    ),
                ),
                RegistrertUtbetaling(
                    utbetaltTil = Aktør.Person(fnr),
                    utbetalingsmetode = "Til konto",
                    utbetalingsstatus = "something",
                    posteringsdato = 9.september(2025),
                    forfallsdato = 19.september(2025),
                    utbetalingsdato = null,
                    nettobeløp = "8700".toBigDecimal(),
                    melding = null,
                    ytelser = listOf(
                        RegistrertYtelse(
                            ytelsestype = "Tiltakspenger",
                            periode = Periode(1.september(2025), 14.september(2025)),
                            nettobeløp = "2850".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "0".toBigDecimal(),
                            trekksum = "0".toBigDecimal(),
                            komponentsum = "2850".toBigDecimal(),
                            komponenter = listOf(
                                Ytelseskomponent(
                                    type = "Tiltakspenger",
                                    satsbeløp = "285".toBigDecimal(),
                                    satstype = "Dag",
                                    satsantall = 10.0,
                                    beløp = "2850".toBigDecimal(),
                                ),
                            ),
                            trekk = emptyList(),
                            skattetrekk = emptyList(),
                            bilagsnummer = null,
                            refundertFor = null,
                        ),
                        RegistrertYtelse(
                            ytelsestype = "Dagpenger",
                            periode = Periode(1.september(2025), 30.september(2025)),
                            nettobeløp = "4200".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "0".toBigDecimal(),
                            trekksum = "0".toBigDecimal(),
                            komponentsum = "4200".toBigDecimal(),
                            komponenter = listOf(
                                Ytelseskomponent(
                                    type = "Dagpenger",
                                    satsbeløp = null,
                                    satstype = null,
                                    satsantall = null,
                                    beløp = "4200".toBigDecimal(),
                                ),
                            ),
                            trekk = emptyList(),
                            skattetrekk = emptyList(),
                            bilagsnummer = null,
                            refundertFor = null,
                        ),
                        RegistrertYtelse(
                            ytelsestype = "Økonomisk sosialhjelp",
                            periode = Periode(1.september(2025), 30.september(2025)),
                            nettobeløp = "1650".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "0".toBigDecimal(),
                            trekksum = "0".toBigDecimal(),
                            komponentsum = "1650".toBigDecimal(),
                            komponenter = emptyList(),
                            trekk = emptyList(),
                            skattetrekk = emptyList(),
                            bilagsnummer = null,
                            refundertFor = null,
                        ),
                        RegistrertYtelse(
                            ytelsestype = null,
                            periode = Periode(18.august(2025), 31.august(2025)),
                            nettobeløp = "0".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "0".toBigDecimal(),
                            trekksum = "0".toBigDecimal(),
                            komponentsum = "0".toBigDecimal(),
                            komponenter = emptyList(),
                            trekk = emptyList(),
                            skattetrekk = emptyList(),
                            bilagsnummer = null,
                            refundertFor = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `rå request, rå respons og status følger med ut`() {
        val svar = UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr)

        runTest {
            val response = hent(transportMed(svar)).getOrFail()

            response.statusCode shouldBe 200
            response.rawResponseString shouldEqualJson svar
            response.rawRequestString.substringAfter("\n\n") shouldEqualJson forventetRequest("YTELSESPERIODE")
        }
    }

    /** Kilden utelater tomme lister og felt uten verdi fra json-en. */
    @Test
    fun `utelatte lister blir tomme og negative summer bevares`() {
        runTest {
            val utbetaling = hent(transportMed(UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr))).getOrFail().body.single()

            utbetaling.nettobeløp shouldBe null
            utbetaling.forfallsdato shouldBe null
            utbetaling.utbetalingsdato shouldBe null
            utbetaling.melding shouldBe null

            val ytelse = utbetaling.ytelser.single()
            ytelse.komponenter shouldBe emptyList()
            ytelse.trekk shouldBe emptyList()
            ytelse.skattetrekk shouldBe emptyList()
            ytelse.bilagsnummer shouldBe null
            ytelse.refundertFor shouldBe null
            ytelse.skattesum shouldBe "-99.9".toBigDecimal()
            ytelse.trekksum shouldBe "-900.75".toBigDecimal()
        }
    }

    @Test
    fun `aktoerId leses som ident`() {
        runTest {
            hent(transportMed(UtbetalingsoversiktDtoTestEx.svarMedAktoerIdAlias(fnr))).getOrFail().body.single().utbetaltTil shouldBe
                Aktør.Person(fnr)
        }
    }

    @Test
    fun `tomt array gir tom liste`() {
        runTest {
            hent(transportMed("[]")).getOrFail().body shouldBe emptyList()
        }
    }

    @Test
    fun `svar som bryter kontrakten gir UgyldigInnhold med feilen og responsens metadata`() {
        val svar = UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr).replace("\"PERSON\"", "\"ROBOT\"")

        runTest {
            val feil = hent(transportMed(svar)).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold>()

            feil.feil shouldBe UtbetalingsoversiktMappingfeil.UkjentAktørtype("utbetaltTil")
            feil.metadata.statusCode shouldBe 200
            feil.metadata.rawResponseString.shouldNotBeNull() shouldEqualJson svar
        }
    }

    /** httpklient gjør ett forsøk til med ferskt token når tjenesten svarer 401. */
    @Test
    fun `401 gir TilgangAvvist etter ett nytt forsøk med ferskt token`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatusForAlleForsøk(statusCode = 401, body = "avvist", contentType = "text/plain", maksForsøk = 2)
        }

        runTest {
            hent(transport).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist>()
                .httpKlientError.statusCode shouldBe 401
            transport.mottatteKall.size shouldBe 2
        }
    }

    @Test
    fun `403 gir TilgangAvvist`() {
        val transport = transportMedStatus(403)

        runTest {
            hent(transport).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist>()
                .httpKlientError.statusCode shouldBe 403
            transport.mottatteKall.size shouldBe 1
        }
    }

    @Test
    fun `400 gir SakAvvist`() {
        runTest {
            hent(transportMedStatus(400)).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.SakAvvist>()
                .httpKlientError.statusCode shouldBe 400
        }
    }

    @Test
    fun `andre statuser gir Tjenestefeil`() {
        listOf(404, 429, 500, 503).forEach { status ->
            withClue(status) {
                runTest {
                    hent(transportMedStatus(status)).leftOrNull().shouldNotBeNull()
                        .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil>()
                        .httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
                        .statusCode shouldBe status
                }
            }
        }
    }

    @Test
    fun `json som ikke lar seg lese gir UleseligSvar`() {
        runTest {
            hent(transportMed("""{"ikke": "en liste"}""")).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.UleseligSvar>()
        }
    }

    @Test
    fun `nettverksfeil gir Tjenestefeil`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(IOException("forbindelsen ble brutt")) }

        runTest {
            hent(transport).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil>()
                .httpKlientError.shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }

    @Test
    fun `feil ved henting av token gir Tjenestefeil uten at kallet sendes`() {
        val transport = FakeHttpTransport()
        val utenToken = object : AuthTokenProvider {
            override suspend fun hentToken(skipCache: Boolean): AccessToken = throw IllegalStateException("ingen token")
        }

        runTest {
            hent(transport, klient = klient(transport, utenToken)).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil>()
                .httpKlientError.shouldBeInstanceOf<HttpKlientError.RequestIkkeSendt>()
            transport.mottatteKall shouldBe emptyList()
        }
    }

    @Test
    fun `sender ident, rolle, periode og periodetype`() {
        listOf(
            Oppslagsperiodetype.UTBETALINGSPERIODE to "UTBETALINGSPERIODE",
            Oppslagsperiodetype.YTELSESPERIODE to "YTELSESPERIODE",
        ).forEach { (periodetype, forventetVerdi) ->
            withClue(periodetype.toString()) {
                val transport = transportMed("[]")

                runTest { hent(transport, periodetype) }

                val kall = transport.mottatteKall.single()
                kall.metode shouldBe "POST"
                kall.uri.toString() shouldBe "$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"
                kall.request.headers().firstValue("Nav-Call-Id").get() shouldBe correlationId.value
                kall.bodyTekst shouldEqualJson forventetRequest(forventetVerdi)
            }
        }
    }

    //language=json
    private fun forventetRequest(periodetype: String) = """
        {
          "ident": "${fnr.verdi}",
          "rolle": "RETTIGHETSHAVER",
          "periode": {
            "fom": "2025-08-01",
            "tom": "2025-09-30"
          },
          "periodetype": "$periodetype"
        }
    """.trimIndent()
}
