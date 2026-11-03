package no.nav.tiltakspenger.saksbehandling.dagpenger.infra.http

import arrow.core.right
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Instant

class DpDatadelingHttpClientTest {
    private val baseUrl = "http://dp-datadeling.test"
    private val correlationId = CorrelationId.generate()
    private val fnr = Fnr.fromString("12345678901")
    private val periode = 1.januar(2025) til 31.mars(2025)

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = DpDatadelingHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    private fun svar(vararg perioder: String) = """
        {
          "personIdent": "12345678901",
          "perioder": [${perioder.joinToString(",")}]
        }
    """.trimIndent()

    private fun periodeJson(ytelseType: String, kilde: String, tilOgMedDato: String?) = """
        {
          "fraOgMedDato": "2025-01-01",
          "tilOgMedDato": ${tilOgMedDato?.let { "\"$it\"" }},
          "ytelseType": "$ytelseType",
          "kilde": "$kilde"
        }
    """.trimIndent()

    @Test
    fun `bygger default HttpKlient når transport ikke sendes inn`() {
        DpDatadelingHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `200 gir dagpengeperiodene og POSTer personIdent og perioden til perioder-endepunktet`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                svar(
                    periodeJson("DAGPENGER_ARBEIDSSOKER_ORDINAER", "ARENA", "2025-02-28"),
                    periodeJson("DAGPENGER_PERMITTERING_ORDINAER", "DP_SAK", null),
                    periodeJson("DAGPENGER_PERMITTERING_FISKEINDUSTRI", "DP_SAK", null),
                ),
                statusCode = 200,
            )
        }

        runTest {
            client(transport).hentDagpengeperioder(fnr, periode, correlationId) shouldBe listOf(
                Dagpengeperiode(
                    fraOgMed = 1.januar(2025),
                    tilOgMed = 28.februar(2025),
                    ytelsetype = Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER,
                    kilde = Dagpengeperiode.Kilde.ARENA,
                ),
                Dagpengeperiode(
                    fraOgMed = 1.januar(2025),
                    tilOgMed = null,
                    ytelsetype = Dagpengeperiode.Ytelsetype.PERMITTERING,
                    kilde = Dagpengeperiode.Kilde.DP_SAK,
                ),
                Dagpengeperiode(
                    fraOgMed = 1.januar(2025),
                    tilOgMed = null,
                    ytelsetype = Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI,
                    kilde = Dagpengeperiode.Kilde.DP_SAK,
                ),
            ).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/dagpenger/datadeling/v1/perioder"
        kall.request.headers().allValues("Nav-Call-Id") shouldBe listOf(correlationId.value)
        kall.bodyTekst shouldEqualJson """
            {
              "personIdent": "12345678901",
              "fraOgMedDato": "2025-01-01",
              "tilOgMedDato": "2025-03-31"
            }
        """.trimIndent()
    }

    @Test
    fun `200 uten perioder gir tom liste`() {
        val transport = FakeHttpTransport().apply { leggIKøJson(svar(), statusCode = 200) }

        runTest {
            client(transport).hentDagpengeperioder(fnr, periode, correlationId) shouldBe emptyList<Dagpengeperiode>().right()
        }
    }

    @Test
    fun `ukjent ytelseType gir Left`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(svar(periodeJson("DAGPENGER_NOE_NYTT", "ARENA", null)), statusCode = 200)
        }

        runTest {
            client(transport).hentDagpengeperioder(fnr, periode, correlationId).isLeft() shouldBe true
        }
    }

    @Test
    fun `ukjent kilde gir Left`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(svar(periodeJson("DAGPENGER_ARBEIDSSOKER_ORDINAER", "NYTT_FAGSYSTEM", null)), statusCode = 200)
        }

        runTest {
            client(transport).hentDagpengeperioder(fnr, periode, correlationId).isLeft() shouldBe true
        }
    }

    @Test
    fun `uventet status gir Left`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain") }

        runTest {
            client(transport).hentDagpengeperioder(fnr, periode, correlationId)
                .leftOrNull().shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    @Test
    fun `fødselsnummeret maskeres i toString`() {
        DagpengeperioderResponse(personIdent = "12345678901", perioder = emptyList()).toString() shouldNotContain "12345678901"
    }
}
