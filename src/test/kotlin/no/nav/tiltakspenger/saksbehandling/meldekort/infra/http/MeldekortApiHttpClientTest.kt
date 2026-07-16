package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Clock

/**
 * Wiremock-test for [MeldekortApiHttpClient].
 *
 * Selve mappingen til DTO-en (og dermed payloaden) er uttømmende verifisert i
 * [SakTilMeldekortApiDTOMapperTest], så her fokuserer vi kun på HTTP-flyten:
 *  - Suksessresponser (2xx) returnerer [Unit] som [arrow.core.Either.Right].
 *  - Feilresponser (utenfor 2xx) returnerer en [arrow.core.Either.Left].
 *  - Exceptions ved utsending blir også returnert som [arrow.core.Either.Left].
 *
 * Tilsammen dekker dette alle linjene i [MeldekortApiHttpClient] (verifisert via Kover).
 */
internal class MeldekortApiHttpClientTest {

    private fun nySak() = ObjectMother.nySakMedVedtak().first

    private fun nyClient(baseUrl: String) = MeldekortApiHttpClient(
        baseUrl = baseUrl,
        clock = Clock.systemUTC(),
        authTokenProvider = object : no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider {
            override suspend fun hentToken(skipCache: Boolean) = ObjectMother.accessToken()
        },
    )

    @Test
    fun `2xx-respons gir Right(Unit) og sender Bearer-token og JSON-payload`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/saksbehandling/sak"
                headers contains "Authorization" equalTo "Bearer token"
                headers contains "Content-Type" equalTo "application/json"
            } returns {
                statusCode = 202
            }

            val client = nyClient(wiremock.baseUrl())

            runTest {
                val result = client.sendSak(nySak())
                result.isRight() shouldBe true
            }
        }
    }

    @Test
    fun `non-2xx respons gir Left`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/saksbehandling/sak"
            } returns {
                statusCode = 500
                body = "noe feilet"
            }

            val client = nyClient(wiremock.baseUrl())

            runTest {
                val result = client.sendSak(nySak())
                result.isLeft() shouldBe true
            }
        }
    }

    @Test
    fun `exception under utsending gir Left`() {
        val client = nyClient("http://localhost:1")

        runTest {
            val result = client.sendSak(nySak())
            result.isLeft() shouldBe true
        }
    }
}
