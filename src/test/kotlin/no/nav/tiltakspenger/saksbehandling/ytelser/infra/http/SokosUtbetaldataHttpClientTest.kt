package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.right
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.libs.httpklient.HttpMethod
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

internal class SokosUtbetaldataHttpClientTest {
    private val baseUrl = "http://utbetaldata.test"
    private val correlationId = CorrelationId.generate()

    private val fnr = Fnr.random()
    private val fom = LocalDate.of(2024, 12, 1)
    private val tom = LocalDate.of(2024, 12, 31)
    private val periode = Periode(fraOgMed = fom, tilOgMed = tom)

    private fun client(httpKlient: HttpKlientFake) = SokosUtbetaldataHttpClient(
        baseUrl = baseUrl,
        getToken = { AccessToken("token", Instant.MAX) },
        clock = ObjectMother.clock,
        httpKlient = httpKlient,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        SokosUtbetaldataHttpClient(
            baseUrl = baseUrl,
            getToken = { AccessToken("token", Instant.MAX) },
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `henter ytelser - 200 gir liste og POSTer til utbetaldata-endepunktet`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueResponse(
                body = listOf(
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
            client(httpKlient).hentYtelserFraUtbetaldata(fnr, periode, correlationId) shouldBe listOf(
                Ytelse(ytelsetype = Ytelsetype.AAP, perioder = listOf(Periode(fom, tom))),
            ).right()
        }

        val request = httpKlient.requests.single()
        request.method shouldBe HttpMethod.POST
        request.uri.toString() shouldBe "$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"
    }

    @Test
    fun `henter ytelser - uventet status gir Left`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            val result = client(httpKlient).hentYtelserFraUtbetaldata(fnr, periode, correlationId)
            result.isLeft() shouldBe true
        }
    }
}
