package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakDb
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

/**
 * Enhetstest for [DatadelingHttpClient] med `FakeHttpTransport` — hele den reelle pipelinen kjører.
 *
 * Selve serialiseringen av payloadene er dekket av de respektive JSON-testene, så her verifiserer vi HTTP-flyten som denne klienten eier: at riktig endepunkt/metode kalles for hver `send`-variant, at suksess gir [arrow.core.Either.Right] og at en [no.nav.tiltakspenger.libs.httpklient.HttpKlientError] fra libs videreformidles uendret som [arrow.core.Either.Left].
 * Testene gir full linjedekning (jf. Kover-regelen i build.gradle.kts).
 */
internal class DatadelingHttpClientTest {

    private val baseUrl = "http://datadeling.test"
    private val correlationId = CorrelationId.generate()

    private val fakeAuthTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = DatadelingHttpClient(
        baseUrl = baseUrl,
        clock = ObjectMother.clock,
        authTokenProvider = fakeAuthTokenProvider,
        transport = transport,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        DatadelingHttpClient(
            baseUrl = baseUrl,
            clock = ObjectMother.clock,
            authTokenProvider = fakeAuthTokenProvider,
        )
    }

    @Test
    fun `send rammevedtak - 200 gir Right(Unit) og POSTer til vedtaks-endepunktet`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }

        runTest {
            client(transport).send(ObjectMother.nyRammevedtakInnvilgelse(), correlationId) shouldBe Unit.right()
        }

        transport.assertPostTil("$baseUrl/vedtak")
    }

    @Test
    fun `send rammevedtak - uventet status videreformidler HttpKlientError som Left`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "noe feilet", contentType = "text/plain") }

        runTest {
            val result = client(transport).send(ObjectMother.nyRammevedtakInnvilgelse(), correlationId)
            result.leftOrNull().shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        }
    }

    @Test
    fun `send rammevedtak - nettverksfeil videreformidler HttpKlientError som Left`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.io.IOException("simulert nettverksfeil")) }

        runTest {
            val result = client(transport).send(ObjectMother.nyRammevedtakInnvilgelse(), correlationId)
            result.leftOrNull().shouldBeInstanceOf<HttpKlientError>()
        }
    }

    @Test
    fun `send rammebehandling - POSTer til behandlings-endepunktet`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }
        val rammebehandling = ObjectMother.nyRammevedtakInnvilgelse().rammebehandling

        runTest {
            client(transport).send(rammebehandling, correlationId) shouldBe Unit.right()
        }

        transport.assertPostTil("$baseUrl/behandling")
    }

    @Test
    fun `send meldekortbehandling - POSTer til behandlings-endepunktet`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }
        val meldekortbehandling = ObjectMother.meldekortBehandletManuelt()

        runTest {
            client(transport).send(meldekortbehandling, correlationId) shouldBe Unit.right()
        }

        transport.assertPostTil("$baseUrl/behandling")
    }

    @Test
    fun `send behandling - ukjent type kaster`() {
        val transport = FakeHttpTransport()
        val ukjentBehandling = mockk<AttesterbarBehandling> {
            every { id } returns ObjectMother.nyRammevedtakInnvilgelse().rammebehandling.id
        }

        shouldThrow<IllegalStateException> {
            runTest { client(transport).send(ukjentBehandling, correlationId) }
        }
    }

    @Test
    fun `send meldeperioder - POSTer til meldeperioder-endepunktet`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }
        val meldeperioder = listOf(ObjectMother.meldeperiode())

        runTest {
            client(transport).send(meldeperioder, correlationId) shouldBe Unit.right()
        }

        transport.assertPostTil("$baseUrl/meldeperioder")
    }

    @Test
    fun `send meldeperioder - tom liste kaster`() {
        val transport = FakeHttpTransport()

        shouldThrow<IllegalStateException> {
            runTest { client(transport).send(emptyList(), correlationId) }
        }
    }

    @Test
    fun `send meldekortvedtak - journalført POSTer til meldekort-endepunktet`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }
        val meldekortvedtak = ObjectMother.meldekortvedtak(journalpostId = JournalpostId("journalpostId"))

        runTest {
            client(transport).send(meldekortvedtak, differansePerKjede = emptyMap(), correlationId) shouldBe Unit.right()
        }

        transport.assertPostTil("$baseUrl/meldekort")
    }

    @Test
    fun `send meldekortvedtak - ikke journalført kaster`() {
        val transport = FakeHttpTransport()
        val meldekortvedtak = ObjectMother.meldekortvedtak(journalpostId = null)

        shouldThrow<IllegalArgumentException> {
            runTest { client(transport).send(meldekortvedtak, differansePerKjede = null, correlationId) }
        }
    }

    @Test
    fun `send sak - POSTer til sak-endepunktet`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }
        val sakDb = SakDb(
            id = SakId.random(),
            fnr = Fnr.random(),
            saksnummer = Saksnummer.genererSaknummer(clock = ObjectMother.clock, løpenr = "1001"),
            sistEndret = LocalDateTime.now(),
            opprettet = LocalDateTime.now(),
        )

        runTest {
            client(transport).send(sakDb, correlationId) shouldBe Unit.right()
        }

        transport.assertPostTil("$baseUrl/sak")
    }

    private fun FakeHttpTransport.assertPostTil(forventetUri: String) {
        val kall = mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe forventetUri
    }
}
