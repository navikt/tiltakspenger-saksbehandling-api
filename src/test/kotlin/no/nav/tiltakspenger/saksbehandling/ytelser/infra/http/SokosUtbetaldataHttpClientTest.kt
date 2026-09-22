package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.right
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
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
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
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

    private fun client(transport: FakeHttpTransport, clock: Clock = ObjectMother.clock) = SokosUtbetaldataHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = clock,
        transport = transport,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        SokosUtbetaldataHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `henter ytelser - 200 gir liste og POSTer til utbetaldata-endepunktet`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                listOf(
                    UtbetalingDto(
                        ytelseListe = listOf(
                            UtbetalingDto.YtelseDto(
                                ytelsestype = Ytelsetype.AAP.tekstverdi,
                                ytelsesperiode = UtbetalingDto.UtbetalingsperiodeDto(fom = fom, tom = tom),
                            ),
                        ),
                    ),
                ),
                statusCode = 200,
            )
        }

        runTest {
            client(transport).hentYtelserFraUtbetaldata(fnr, periode, correlationId) shouldBe listOf(
                Ytelse(ytelsetype = Ytelsetype.AAP, perioder = listOf(Periode(fom, tom))),
            ).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"
    }

    @Test
    fun `henter ytelser - null ytelsestype mappes til UKJENT`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                listOf(
                    UtbetalingDto(
                        ytelseListe = listOf(
                            UtbetalingDto.YtelseDto(
                                ytelsestype = null,
                                ytelsesperiode = UtbetalingDto.UtbetalingsperiodeDto(fom = fom, tom = tom),
                            ),
                        ),
                    ),
                ),
                statusCode = 200,
            )
        }

        runTest {
            client(transport).hentYtelserFraUtbetaldata(fnr, periode, correlationId) shouldBe listOf(
                Ytelse(ytelsetype = Ytelsetype.UKJENT, perioder = listOf(Periode(fom, tom))),
            ).right()
        }
    }

    /**
     * Regresjonsvakt for utvidelsen av [UtbetalingDto].
     *
     * Svaret har alle nivåene spec-en beskriver, og alt utenom `ytelsestype` og `ytelsesperiode` er felter DTO-en ikke leser i dag.
     * Resultatet pinnes eksplisitt: ett [Ytelse]-element per ytelsestype, med periodene i den rekkefølgen de kommer i svaret, ukjente typer filtrert bort og `null`-typen som [Ytelsetype.UKJENT].
     */
    @Test
    fun `henter ytelser - rikt svar gir samme ytelser som i dag`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(UtbetalingDtoTestEx.riktSvar(fnr), statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(fnr, fixturperiode, correlationId) shouldBe listOf(
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
                .hentYtelserFraUtbetaldata(fnr, fixturperiode, correlationId) shouldBe
                UtbetalingDtoTestEx.forventedeYtelserFraRiktSvar.right()
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
                .hentYtelserFraUtbetaldata(fnr, fixturperiode, correlationId) shouldBe listOf(
                Ytelse(
                    ytelsetype = Ytelsetype.TILTAKSPENGER,
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
                .hentYtelserFraUtbetaldata(fnr, fixturperiode, correlationId) shouldBe emptyList<Ytelse>().right()
        }
    }

    /**
     * Pinner requesten utbetaldata faktisk får.
     *
     * `rolle` og `periodetype` har defaultverdier i [HentUtbetalingsinformasjonRequest] og er derfor usynlige på kallstedet.
     * Endres en av dem, endres hvilke rader vi får tilbake — uten at noen mapping feiler.
     */
    @Test
    fun `henter ytelser - sender ident rolle periode og periodetype på tråden`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("[]", statusCode = 200)
        }

        runTest {
            client(transport, klokkeEtterFixturperioden)
                .hentYtelserFraUtbetaldata(fnr, fixturperiode, correlationId)
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
                client(transport).hentYtelserFraUtbetaldata(fnr, fremtidigPeriode, correlationId)
            }
        }

        transport.mottatteKall shouldBe emptyList()
    }
}
