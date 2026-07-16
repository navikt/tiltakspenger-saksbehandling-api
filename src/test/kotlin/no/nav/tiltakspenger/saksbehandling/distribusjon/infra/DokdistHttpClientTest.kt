package no.nav.tiltakspenger.saksbehandling.distribusjon.infra

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
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

    private fun client(transport: FakeHttpTransport) = DokdistHttpClient(
        baseUrl = baseUrl,
        clock = ObjectMother.clock,
        authTokenProvider = fakeAuthTokenProvider,
        transport = transport,
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
        val transport = FakeHttpTransport().apply { leggIKøJson(DokdistResponse(bestillingsId = "bestillingsId"), statusCode = 200) }

        runTest {
            client(transport).distribuerDokument(journalpostId, correlationId) shouldBe DistribusjonId("bestillingsId").right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe distribuerUri
        kall.request.headers().allValues("Nav-CallId") shouldBe listOf(correlationId.value)
    }

    @Test
    fun `409 regnes som suksess og gir Right med distribusjonId`() {
        val transport = FakeHttpTransport().apply { leggIKøJson(DokdistResponse(bestillingsId = "bestillingsId"), statusCode = 409) }

        runTest {
            client(transport).distribuerDokument(journalpostId, correlationId) shouldBe DistribusjonId("bestillingsId").right()
        }
    }

    @Test
    fun `uventet status videreformidler HttpKlientError som Left`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "noe feilet", contentType = "text/plain") }

        runTest {
            val result = client(transport).distribuerDokument(journalpostId, correlationId)
            result.leftOrNull().shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        }
    }
}
