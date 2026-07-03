package no.nav.tiltakspenger.saksbehandling.distribusjon.infra

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.libs.httpklient.HttpMethod
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Enhetstest for [DokdistHttpClient] med [HttpKlientFake].
 *
 * Vi verifiserer HTTP-flyten som denne klienten eier: at riktig endepunkt/metode kalles med `Nav-CallId`-headeren, at 200 og 409 (forventet ved re-distribusjon) gir [arrow.core.Either.Right] med [DistribusjonId], og at en [HttpKlientError] fra libs videreformidles uendret som [arrow.core.Either.Left].
 * Testene gir full linjedekning (jf. Kover-regelen i build.gradle.kts).
 */
internal class DokdistHttpClientTest {

    private val baseUrl = "http://dokdist.test"
    private val distribuerUri = "$baseUrl/rest/v1/distribuerjournalpost"
    private val correlationId = CorrelationId.generate()
    private val journalpostId = JournalpostId("journalpostId")

    private val fakeAuthTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(httpKlient: HttpKlientFake) = DokdistHttpClient(
        baseUrl = baseUrl,
        clock = ObjectMother.clock,
        authTokenProvider = fakeAuthTokenProvider,
        httpKlient = httpKlient,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        DokdistHttpClient(
            baseUrl = baseUrl,
            clock = ObjectMother.clock,
            authTokenProvider = fakeAuthTokenProvider,
        )
    }

    @Test
    fun `200 gir Right med distribusjonId og POSTer med Nav-CallId`() {
        val httpKlient = HttpKlientFake().apply { enqueueResponse(body = DokdistResponse(bestillingsId = "bestillingsId"), statusCode = 200) }

        runTest {
            client(httpKlient).distribuerDokument(journalpostId, correlationId) shouldBe DistribusjonId("bestillingsId").right()
        }

        val request = httpKlient.requests.single()
        request.method shouldBe HttpMethod.POST
        request.uri.toString() shouldBe distribuerUri
        request.headers["Nav-CallId"] shouldBe listOf(correlationId.value)
    }

    @Test
    fun `409 regnes som suksess og gir Right med distribusjonId`() {
        val httpKlient = HttpKlientFake().apply { enqueueResponse(body = DokdistResponse(bestillingsId = "bestillingsId"), statusCode = 409) }

        runTest {
            client(httpKlient).distribuerDokument(journalpostId, correlationId) shouldBe DistribusjonId("bestillingsId").right()
        }
    }

    @Test
    fun `uventet status videreformidler HttpKlientError som Left`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "noe feilet") }

        runTest {
            val result = client(httpKlient).distribuerDokument(journalpostId, correlationId)
            result.leftOrNull().shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        }
    }
}
