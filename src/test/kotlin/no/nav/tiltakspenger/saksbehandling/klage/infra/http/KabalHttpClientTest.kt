package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class KabalHttpClientTest {

    @Test
    fun `håndterer ok request`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/api/oversendelse/v4/sak"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
            }
            val kabalclient = KabalHttpClient(
                baseUrl = wiremock.baseUrl(),
                getToken = { ObjectMother.accessToken() },
            )

            runTest {
                kabalclient.oversend(
                    klagebehandling = ObjectMother.opprettKlagebehandling(),
                    journalpostIdVedtak = JournalpostId("journalpost-vedtak-1"),
                ) shouldBe Unit.right()
            }
        }
    }
}
