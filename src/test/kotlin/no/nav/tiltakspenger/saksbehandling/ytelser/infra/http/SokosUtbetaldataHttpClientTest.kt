package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.right
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Fødselsnumre
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.request.SokosHentUtbetalingsinformasjonRequest
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.SokosUtbetalingResponse
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class SokosUtbetaldataHttpClientTest {
    private val baseUrl = "http://utbetaldata.test"
    private val correlationId = CorrelationId.generate()

    private val fnr = Fnr.random()
    private val fom = LocalDate.of(2024, 12, 1)
    private val tom = LocalDate.of(2024, 12, 31)
    private val periode = Periode(fraOgMed = fom, tilOgMed = tom)

    /**
     * Fixturen i [UtbetalingDtoTestEx] bruker spec-ens egne datoer i august og september 2025.
     * Klienten nekter å spørre om datoer frem i tid, så testene som bruker fixturen trenger en klokke etter perioden de spør om.
     */
    private val klokkeEtterFixturperioden: Clock = fixedClockAt(1.oktober(2025))
    private val fixturperiode = Periode(fraOgMed = 1.august(2025), tilOgMed = 30.september(2025))

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(
        transport: FakeHttpTransport,
        clock: Clock = ObjectMother.clock,
        meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    ) = SokosUtbetaldataHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = clock,
        meterRegistry = meterRegistry,
        transport = transport,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        SokosUtbetaldataHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
            meterRegistry = SimpleMeterRegistry(),
        )
    }

    @Test
    fun `henter ytelser - 200 gir liste og POSTer til utbetaldata-endepunktet`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                listOf(
                    SokosUtbetalingResponse(
                        ytelseListe = listOf(
                            SokosUtbetalingResponse.YtelseDto(
                                ytelsestype = "Arbeidsavklaringspenger",
                                ytelsesperiode = SokosUtbetalingResponse.UtbetalingsperiodeDto(fom = fom, tom = tom),
                            ),
                        ),
                    ),
                ),
                statusCode = 200,
            )
        }

        runTest {
            val svar = client(transport).hentYtelserFraUtbetaldata(Fødselsnumre(fnr), periode, correlationId)
            svar.map { it.ytelser } shouldBe listOf(
                Ytelse.Kjent(ytelsetype = Ytelsetype.AAP, kildetekst = "Arbeidsavklaringspenger", perioder = listOf(Periode(fom, tom))),
            ).right()
            svar.getOrNull()!!.metadata.single().svar shouldContain "\"ytelsestype\":\"Arbeidsavklaringspenger\""
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"
    }

    @Test
    fun `slår opp hver ident etter tur og slår sammen ytelsene`() {
        val tidligereFnr = Fnr.random()
        val senere = Periode(tom.plusDays(1), tom.plusDays(10))

        fun svarMed(periode: Periode): List<SokosUtbetalingResponse> {
            return listOf(
                SokosUtbetalingResponse(
                    ytelseListe = listOf(
                        SokosUtbetalingResponse.YtelseDto(
                            ytelsestype = "Arbeidsavklaringspenger",
                            ytelsesperiode = SokosUtbetalingResponse.UtbetalingsperiodeDto(fom = periode.fraOgMed, tom = periode.tilOgMed),
                        ),
                    ),
                ),
            )
        }
        val transport = FakeHttpTransport().apply {
            leggIKøJson(svarMed(periode), statusCode = 200)
            leggIKøJson(svarMed(senere), statusCode = 200)
        }

        runTest {
            val svar = client(transport).hentYtelserFraUtbetaldata(Fødselsnumre(fnr, listOf(tidligereFnr)), periode, correlationId).getOrNull()!!

            svar.ytelser shouldBe listOf(
                Ytelse.Kjent(ytelsetype = Ytelsetype.AAP, kildetekst = "Arbeidsavklaringspenger", perioder = listOf(periode, senere)),
            )
            svar.metadata.size shouldBe 2
        }
        transport.mottatteKall.map { objectMapper.readTree(it.bodyTekst)["ident"].asText() } shouldBe listOf(fnr.verdi, tidligereFnr.verdi)
    }

    @Test
    fun `stopper ved første ident som feiler`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain")
        }

        runTest {
            client(transport).hentYtelserFraUtbetaldata(Fødselsnumre(fnr, listOf(Fnr.random())), periode, correlationId).isLeft() shouldBe true
        }
        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `henter ytelser - null ytelsestype blir en ukjent ytelse`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                listOf(
                    SokosUtbetalingResponse(
                        ytelseListe = listOf(
                            SokosUtbetalingResponse.YtelseDto(
                                ytelsestype = null,
                                ytelsesperiode = SokosUtbetalingResponse.UtbetalingsperiodeDto(fom = fom, tom = tom),
                            ),
                        ),
                    ),
                ),
                statusCode = 200,
            )
        }

        runTest {
            client(transport).hentYtelserFraUtbetaldata(Fødselsnumre(fnr), periode, correlationId).map { it.ytelser } shouldBe listOf(
                Ytelse.Ukjent(kildetekst = null, perioder = listOf(Periode(fom, tom))),
            ).right()
        }
    }

    /**
     * Regresjonsvakt for utvidelsen av [SokosUtbetalingResponse].
     *
     * Svaret har alle nivåene spec-en beskriver, og alt utenom `ytelsestype` og `ytelsesperiode` er felter DTO-en ikke leser i dag.
     * Resultatet pinnes eksplisitt: ett [Ytelse]-element per ytelsestype, med periodene i den rekkefølgen de kommer i svaret, og ukjente typer og `null`-typen som [Ytelse.Ukjent].
     */
    @Test
    fun `henter ytelser - rikt svar gir samme ytelser som i dag`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(UtbetalingDtoTestEx.riktSvar(fnr), statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fixturperiode, correlationId).map { it.ytelser } shouldBe listOf(
                Ytelse.Kjent(
                    ytelsetype = Ytelsetype.TILTAKSPENGER,
                    kildetekst = "Tiltakspenger",
                    perioder = listOf(
                        Periode(1.august(2025), 14.august(2025)),
                        Periode(1.september(2025), 14.september(2025)),
                    ),
                ),
                Ytelse.Kjent(
                    ytelsetype = Ytelsetype.DAGPENGER,
                    kildetekst = "Dagpenger",
                    perioder = listOf(Periode(1.september(2025), 30.september(2025))),
                ),
                Ytelse.Ukjent(
                    kildetekst = "Økonomisk sosialhjelp",
                    perioder = listOf(Periode(1.september(2025), 30.september(2025))),
                ),
                Ytelse.Ukjent(
                    kildetekst = null,
                    perioder = listOf(Periode(18.august(2025), 31.august(2025))),
                ),
            ).right()
        }
    }

    /** Samme forventning uttrykt via fixturen, slik at e2e-testen og klienttesten deler én kilde til hva det rike svaret betyr. */
    @Test
    fun `henter ytelser - rikt svar gir ytelsene fixturen lover`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(UtbetalingDtoTestEx.riktSvar(fnr), statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fixturperiode, correlationId).map { it.ytelser } shouldBe
                UtbetalingDtoTestEx.forventedeYtelserFraRiktSvar.right()
        }
    }

    @Test
    fun `henter ytelser - ytelsestyper vi ikke kjenner, telles per type`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(UtbetalingDtoTestEx.riktSvar(fnr), statusCode = 200)
        }
        val meterRegistry = SimpleMeterRegistry()

        runTest {
            client(transport, klokkeEtterFixturperioden, meterRegistry).hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fixturperiode, correlationId)

            meterRegistry.find("tpts_saksbehandlingapi_utbetaldata_ukjente_ytelser").counters()
                .associate { it.id.getTag("ytelsestype") to it.count() } shouldBe mapOf("Økonomisk sosialhjelp" to 1.0)
        }
    }

    /** Et svar med bare feltene dagens DTO kjenner, skal fortsatt mappes likt etter utvidelsen. */
    @Test
    fun `henter ytelser - minimalt svar gir samme ytelser som i dag`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(UtbetalingDtoTestEx.minimaltSvar(), statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fixturperiode, correlationId).map { it.ytelser } shouldBe listOf(
                Ytelse.Kjent(
                    ytelsetype = Ytelsetype.TILTAKSPENGER,
                    kildetekst = "Tiltakspenger",
                    perioder = listOf(Periode(1.august(2025), 14.august(2025))),
                ),
            ).right()
        }
    }

    @Test
    fun `henter ytelser - tomt array gir tom liste`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("[]", statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fixturperiode, correlationId).map { it.ytelser } shouldBe emptyList<Ytelse>().right()
        }
    }

    /**
     * Pinner requesten utbetaldata faktisk får.
     *
     * `rolle` og `periodetype` har defaultverdier i [SokosHentUtbetalingsinformasjonRequest] og er derfor usynlige på kallstedet.
     * Endres en av dem, endres hvilke rader vi får tilbake — uten at noen mapping feiler.
     */
    @Test
    fun `henter ytelser - sender ident rolle periode og periodetype på tråden`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("[]", statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fixturperiode, correlationId)
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"
        kall.request.headers().firstValue("Nav-Call-Id").get() shouldBe correlationId.value
        //language=json
        kall.bodyTekst shouldEqualJson """
            {
              "ident": "${fnr.verdi}",
              "rolle": "RETTIGHETSHAVER",
              "periode": {
                "fom": "2025-08-01",
                "tom": "2025-09-30"
              },
              "periodetype": "UTBETALINGSPERIODE"
            }
        """.trimIndent()
    }

    /**
     * Utbetaldata godtar ikke datoer frem i tid.
     * Klienten skal si fra i stedet for å krympe perioden i det stille — domenet lagrer perioden det har spurt om, og ville da lagret feil periode.
     */
    @Test
    fun `henter ytelser - kaster når perioden er frem i tid`() {
        val transport = FakeHttpTransport()
        val fremtidigPeriode = Periode(
            fraOgMed = LocalDate.now(ObjectMother.clock).plusDays(1),
            tilOgMed = LocalDate.now(ObjectMother.clock).plusDays(10),
        )

        runTest {
            shouldThrowWithMessage<IllegalStateException>("Utbetaldata godtar ikke datoer frem i tid.") {
                client(transport).hentYtelserFraUtbetaldata(Fødselsnumre(fnr), fremtidigPeriode, correlationId)
            }
        }

        transport.mottatteKall shouldBe emptyList()
    }
}
