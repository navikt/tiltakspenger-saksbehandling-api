package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.left
import arrow.core.right
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Wiremock-test for [MeldekortApiHttpClient].
 *
 * Selve mappingen til DTO-en (og dermed payloaden) er uttømmende verifisert i
 * [SakTilMeldekortApiDTOMapperTest], så her fokuserer vi kun på HTTP-flyten:
 *  - Suksessresponser (2xx) returnerer [Unit] som [arrow.core.Either.Right].
 *  - Feilresponser (utenfor 2xx) returnerer [FeilVedSendingTilMeldekortApi] som [arrow.core.Either.Left].
 *  - Eksceptions ved utsending (f.eks. ingen tilkobling) blir fanget og mappet til samme [Left].
 *
 * Tilsammen dekker dette alle linjene i [MeldekortApiHttpClient] (verifisert via Kover).
 */
internal class MeldekortApiHttpClientTest {

    private fun nySak() = ObjectMother.nySakMedVedtak().first

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

            val client = MeldekortApiHttpClient(
                baseUrl = wiremock.baseUrl(),
                getToken = { ObjectMother.accessToken() },
            )

            runTest {
                client.sendSak(nySak()) shouldBe Unit.right()
            }
        }
    }

    @Test
    fun `non-2xx respons gir FeilVedSendingTilMeldekortApi`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/saksbehandling/sak"
            } returns {
                statusCode = 500
                body = "noe feilet"
            }

            val client = MeldekortApiHttpClient(
                baseUrl = wiremock.baseUrl(),
                getToken = { ObjectMother.accessToken() },
            )

            runTest {
                client.sendSak(nySak()) shouldBe FeilVedSendingTilMeldekortApi.left()
            }
        }
    }

    @Test
    fun `exception under utsending gir FeilVedSendingTilMeldekortApi`() {
        // Peker mot en port ingen lytter på for å trigge en ConnectException som fanges av Either#catch.
        val client = MeldekortApiHttpClient(
            baseUrl = "http://localhost:1",
            getToken = { ObjectMother.accessToken() },
        )

        runTest {
            client.sendSak(nySak()) shouldBe FeilVedSendingTilMeldekortApi.left()
        }
    }
}
