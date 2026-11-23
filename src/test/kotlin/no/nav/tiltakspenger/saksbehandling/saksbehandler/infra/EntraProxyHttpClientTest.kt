package no.nav.tiltakspenger.saksbehandling.saksbehandler.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.EntraGruppe
import no.nav.tiltakspenger.saksbehandling.saksbehandler.KanIkkeHenteGrupper
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class EntraProxyHttpClientTest {
    private val navIdent = "Z123456"

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("systemtoken", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = EntraProxyHttpClient(
        baseUrl = "http://entra-proxy.test",
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    /**
     * Rå json slik entra-proxy faktisk svarer, med `beskrivelse` som vi ikke modellerer.
     * Da beviser testen at deserialiseringen tåler feltet, i stedet for bare å speile vår egen DTO.
     */
    @Test
    fun `hentGrupper gir gruppene og gjør GET med systemtoken mot tilganger-endepunktet`() = runTest {
        val transport = FakeHttpTransport()
        transport.leggIKøJson(
            json = """
                [
                  {"rolle": "0000-GA-Gruppe-A", "gruppeId": "11111111-1111-1111-1111-111111111111", "beskrivelse": "Gruppe A"},
                  {"rolle": "0000-GA-Gruppe-B", "gruppeId": "22222222-2222-2222-2222-222222222222", "beskrivelse": null}
                ]
            """.trimIndent(),
        )

        val grupper = client(transport).hentGrupper(navIdent).fold({ throw AssertionError(it) }, { it })

        grupper shouldBe setOf(
            EntraGruppe(id = UUID.fromString("11111111-1111-1111-1111-111111111111"), navn = "0000-GA-Gruppe-A"),
            EntraGruppe(id = UUID.fromString("22222222-2222-2222-2222-222222222222"), navn = "0000-GA-Gruppe-B"),
        )
        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "GET"
        kall.uri.toString() shouldBe "http://entra-proxy.test/api/v1/ansatt/tilganger/Z123456"
        kall.request.headers().allValues("Authorization") shouldBe listOf("Bearer systemtoken")
    }

    /** Entra-proxy svarer 200 med tom liste for en ident den ikke finner i Entra. */
    @Test
    fun `hentGrupper gir tomt sett når entra-proxy svarer med tom liste`() = runTest {
        val transport = FakeHttpTransport()
        transport.leggIKøJson(json = "[]")

        client(transport).hentGrupper(navIdent).fold({ throw AssertionError(it) }, { it }) shouldBe emptySet()
    }

    @Test
    fun `hentGrupper encoder identen som ett stisegment`() = runTest {
        val transport = FakeHttpTransport()
        transport.leggIKøJson(json = "[]")

        client(transport).hentGrupper("Z12/../3 4")

        transport.mottatteKall.single().uri.toString() shouldBe
            "http://entra-proxy.test/api/v1/ansatt/tilganger/Z12%2F..%2F3%204"
    }

    @Test
    fun `hentGrupper gir KallFeilet for uventet status`() = runTest {
        val transport = FakeHttpTransport()
        transport.leggIKøStatus(statusCode = 500, body = "intern serverfeil", contentType = "text/plain")

        client(transport).hentGrupper(navIdent).fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<KanIkkeHenteGrupper.KallFeilet>()
            .httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
    }

    @Test
    fun `hentGrupper gir KallFeilet når en gruppe mangler gruppeId`() = runTest {
        val transport = FakeHttpTransport()
        transport.leggIKøJson(json = """[{"rolle": "0000-GA-Gruppe-A", "gruppeId": null}]""")

        client(transport).hentGrupper(navIdent).fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<KanIkkeHenteGrupper.KallFeilet>()
            .httpKlientError.shouldBeInstanceOf<HttpKlientError.DeserializationError>()
    }

    @Test
    fun `bygger produksjonstransport når transport ikke sendes inn`() {
        EntraProxyHttpClient(
            baseUrl = "http://entra-proxy.test",
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }
}
