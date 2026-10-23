package no.nav.tiltakspenger.saksbehandling.søknad.infra.http

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KunneIkkeSendeSøknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfallresultat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

/**
 * Enhetstest for [SøknadApiHttpClient] med `FakeHttpTransport` — hele den reelle pipelinen kjører.
 * Testene gir full linjedekning (jf. Kover-regelen i build.gradle.kts).
 */
class SøknadApiHttpClientTest {

    private val baseUrl = "http://soknad-api.test"

    private val fakeAuthTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = SøknadApiHttpClient(
        baseUrl = baseUrl,
        clock = ObjectMother.clock,
        authTokenProvider = fakeAuthTokenProvider,
        transport = transport,
    )

    private val søknadsutfall = Søknadsutfall(
        søknadId = SøknadId.random(),
        resultat = Søknadsutfallresultat.AVSLAG,
        tidspunkt = LocalDateTime.parse("2025-05-01T12:30:15.123456"),
        vedtakId = VedtakId.random(),
    )

    @Test
    fun `bygger default HttpKlient når transport ikke sendes inn`() {
        SøknadApiHttpClient(
            baseUrl = baseUrl,
            clock = ObjectMother.clock,
            authTokenProvider = fakeAuthTokenProvider,
        )
    }

    @Test
    fun `2xx gir Right og POSTer utfallet til søknad-api`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 204) }

        runTest {
            client(transport).sendSøknadsutfall(søknadsutfall) shouldBe Unit.right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/saksbehandling/soknad/utfall"
        deserialize<Map<String, String>>(kall.bodyTekst) shouldBe mapOf(
            "søknadId" to søknadsutfall.søknadId.toString(),
            "vedtakId" to søknadsutfall.vedtakId.toString(),
            "utfall" to "AVSLAG",
            "tidspunkt" to "2025-05-01T12:30:15.123456",
        )
    }

    @Test
    fun `avbrudd sendes uten vedtakId`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 204) }

        runTest {
            client(transport).sendSøknadsutfall(
                søknadsutfall.copy(resultat = Søknadsutfallresultat.AVBRUTT, vedtakId = null),
            ) shouldBe Unit.right()
        }

        deserialize<Map<String, String?>>(transport.mottatteKall.single().bodyTekst) shouldBe mapOf(
            "søknadId" to søknadsutfall.søknadId.toString(),
            "vedtakId" to null,
            "utfall" to "AVBRUTT",
            "tidspunkt" to "2025-05-01T12:30:15.123456",
        )
    }

    @Test
    fun `hvert resultat sendes med samme navn i kontrakten`() {
        Søknadsutfallresultat.entries.forEach { resultat ->
            val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 204) }

            runTest {
                client(transport).sendSøknadsutfall(
                    søknadsutfall.copy(
                        resultat = resultat,
                        vedtakId = if (resultat.harVedtak) VedtakId.random() else null,
                    ),
                ) shouldBe Unit.right()
            }

            deserialize<Map<String, String?>>(transport.mottatteKall.single().bodyTekst)["utfall"] shouldBe resultat.name
        }
    }

    @Test
    fun `404 betyr at søknad-api ikke kjenner søknaden`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 404) }

        runTest {
            client(transport).sendSøknadsutfall(søknadsutfall).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSendeSøknadsutfall.SøknadFinnesIkke>()
        }
    }

    @Test
    fun `annen uventet status er en teknisk feil`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 400, body = "ugyldig") }

        runTest {
            client(transport).sendSøknadsutfall(søknadsutfall).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSendeSøknadsutfall.Teknisk>()
        }
    }

    @Test
    fun `nettverksfeil er en teknisk feil`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.io.IOException("simulert nettverksfeil")) }

        runTest {
            client(transport).sendSøknadsutfall(søknadsutfall).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSendeSøknadsutfall.Teknisk>()
        }
    }
}
