package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.core.right
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.libs.httpklient.HttpMethod
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

internal class TiltakspengerArenaHttpClientTest {
    private val baseUrl = "http://arena.test"
    private val correlationId = CorrelationId.generate()

    private val fnr = Fnr.random()
    private val periode = no.nav.tiltakspenger.libs.periode.Periode(
        fraOgMed = LocalDate.of(2024, 1, 1),
        tilOgMed = LocalDate.of(2024, 1, 31),
    )

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(httpKlient: HttpKlientFake) = TiltakspengerArenaHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        httpKlient = httpKlient,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        TiltakspengerArenaHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `henter vedtak - 200 gir liste og POSTer til arena-endepunktet`() {
        val vedtak = ArenaTPVedtak(
            fraOgMed = LocalDate.of(2024, 1, 1),
            tilOgMed = LocalDate.of(2024, 1, 31),
            rettighet = ArenaTPVedtak.Rettighet.TILTAKSPENGER,
            vedtakId = 42L,
        )
        val httpKlient = HttpKlientFake().apply {
            enqueueResponse(
                body = listOf(
                    ArenaTPVedtakDto(
                        fraOgMed = vedtak.fraOgMed,
                        tilOgMed = vedtak.tilOgMed,
                        rettighet = vedtak.rettighet,
                        vedtakId = vedtak.vedtakId,
                    ),
                ),
                statusCode = 200,
            )
        }

        runTest {
            client(httpKlient).hentTiltakspengevedtakFraArena(fnr, periode, correlationId) shouldBe listOf(vedtak).right()
        }

        val request = httpKlient.requests.single()
        request.method shouldBe HttpMethod.POST
        request.uri.toString() shouldBe "$baseUrl/azure/tiltakspenger/vedtak"
    }

    @Test
    fun `henter vedtak - uventet status gir Left`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            val result = client(httpKlient).hentTiltakspengevedtakFraArena(fnr, periode, correlationId)
            result.isLeft() shouldBe true
        }
    }
}
