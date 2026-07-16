package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.tilOversendtKlageTilKabalMetadata
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
                clock = fixedClock,
                authTokenProvider = object : AuthTokenProvider {
                    override suspend fun hentToken(skipCache: Boolean) = ObjectMother.accessToken()
                },
            )

            runTest {
                val klagebehandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
                val resultat = kabalclient.oversend(
                    klagebehandling = klagebehandling,
                    journalpostIdVedtak = JournalpostId("journalpost-vedtak-1"),
                )

                val response = resultat.shouldBeInstanceOf<Either.Right<HttpKlientResponse<Unit>>>().value
                response.statusCode shouldBe 200
                response.body shouldBe Unit
                val metadata = response.metadata.tilOversendtKlageTilKabalMetadata(clock = fixedClock)
                metadata.statusKode shouldBe 200
                metadata.response shouldBe ""
                metadata.oversendtTidspunkt shouldBe nå(fixedClock)
                // request lagres som redaktert rå-request; body-en skal fortsatt være med.
                response.rawRequestString shouldContain """"kildeReferanse":"${klagebehandling.id}""""
                response.rawRequestString shouldContain "Authorization: ***"
            }
        }
    }

    @Test
    fun `returnerer feil for uventet statuskode`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/api/oversendelse/v4/sak"
            } returns {
                statusCode = 400
                body = """{"message":"bad request"}"""
                header = "Content-Type" to "application/json"
            }
            val kabalclient = KabalHttpClient(
                baseUrl = wiremock.baseUrl(),
                clock = fixedClock,
                authTokenProvider = object : AuthTokenProvider {
                    override suspend fun hentToken(skipCache: Boolean) = ObjectMother.accessToken()
                },
            )

            runTest {
                val klagebehandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
                val resultat = kabalclient.oversend(
                    klagebehandling = klagebehandling,
                    journalpostIdVedtak = JournalpostId("journalpost-vedtak-1"),
                )

                val feil = resultat.shouldBeInstanceOf<Either.Left<HttpKlientError>>().value
                val responsFeil = feil.shouldBeInstanceOf<HttpKlientError.ResponsMottatt>()
                responsFeil.statusCode shouldBe 400
                responsFeil.body shouldBe """{"message":"bad request"}"""

                // Domenet bygger metadataen fra feilen (mappingen ligger på Metadata-typen).
                val metadata = responsFeil.tilOversendtKlageTilKabalMetadata(clock = fixedClock)
                metadata.statusKode shouldBe 400
                metadata.response shouldBe """{"message":"bad request"}"""
                metadata.oversendtTidspunkt shouldBe nå(fixedClock)
            }
        }
    }

    @Test
    fun `returnerer feil ved transportfeil`() {
        runTest {
            val fakeTransport = FakeHttpTransport().apply {
                leggIKøKast(IllegalStateException("boom"))
            }
            val kabalclient = KabalHttpClient(
                baseUrl = "http://example.com",
                clock = fixedClock,
                authTokenProvider = object : AuthTokenProvider {
                    override suspend fun hentToken(skipCache: Boolean) = ObjectMother.accessToken()
                },
                transport = fakeTransport,
            )

            val klagebehandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
            val resultat = kabalclient.oversend(
                klagebehandling = klagebehandling,
                journalpostIdVedtak = JournalpostId("journalpost-vedtak-1"),
            )

            val feil = resultat.shouldBeInstanceOf<Either.Left<HttpKlientError>>().value
            feil.shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }
}
