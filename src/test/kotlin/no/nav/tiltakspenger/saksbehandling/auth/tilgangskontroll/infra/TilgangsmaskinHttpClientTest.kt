package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollFeil
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponseDto
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant

class TilgangsmaskinHttpClientTest {
    private val texasClient = mockk<TexasClient>()
    private val fakeTransport = FakeHttpTransport()
    private val client = TilgangsmaskinHttpClient(
        baseUrl = "https://tilgangsmaskin.test",
        scope = "scope",
        texasClient = texasClient,
        clock = Clock.systemUTC(),
        transport = fakeTransport,
    )

    @Test
    fun `harTilgangTilPerson returns godkjent for 204`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
        fakeTransport.leggIKøTomRespons(statusCode = 204)

        val result = client.harTilgangTilPerson(Fnr.fromString("01010199999"), "token")

        result.fold({ throw AssertionError(it) }, { it }) shouldBe Tilgangsvurdering.Godkjent
    }

    @Test
    fun `harTilgangTilPerson returns avvist vurdering for 403`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
        fakeTransport.leggIKøJson(
            json = """
                {
                  "type": "https://example.com/type",
                  "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                  "status": 403,
                  "brukerIdent": "01010199999",
                  "navIdent": "Z12345",
                  "begrunnelse": "Du har ikke tilgang"
                }
            """.trimIndent(),
            statusCode = 403,
        )

        val result = client.harTilgangTilPerson(Fnr.fromString("01010199999"), "token")

        val vurdering = result.fold({ throw AssertionError(it) }, { it })
        vurdering shouldBe Tilgangsvurdering.Avvist(
            årsak = no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG,
            begrunnelse = "Du har ikke tilgang",
            metadata = AvvistMetadata(
                type = "https://example.com/type",
                navIdent = "Z12345",
                brukerIdent = "01010199999",
            ),
        )
    }

    @Test
    fun `harTilgangTilPersoner returns bulk response for 207`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
        fakeTransport.leggIKøJson(
            TilgangBulkResponseDto(
                resultater = listOf(
                    TilgangBulkResponseDto.TilgangResponse(
                        brukerId = "01010199999",
                        status = 204,
                    ),
                ),
            ),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(Fnr.fromString("01010199999")), "token")

        result.fold({ throw AssertionError(it) }, { it }) shouldBe mapOf(
            Fnr.fromString("01010199999") to true,
        )
    }

    @Test
    fun `harTilgangTilPerson gir Uventet for andre feilstatuser`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
        fakeTransport.leggIKøStatus(statusCode = 500, body = "intern serverfeil", contentType = "text/plain")

        val result = client.harTilgangTilPerson(Fnr.fromString("01010199999"), "token")

        result.fold({ it }, { throw AssertionError(it) }).shouldBeInstanceOf<TilgangskontrollFeil.Uventet>()
    }

    @Test
    fun `bygger produksjonstransport når transport ikke sendes inn`() {
        TilgangsmaskinHttpClient(
            baseUrl = "https://tilgangsmaskin.test",
            scope = "scope",
            texasClient = texasClient,
            clock = Clock.systemUTC(),
        )
    }

    @Test
    fun `harTilgangTilPerson gir Uventet når token-exchange feiler`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } throws RuntimeException("boom")

        val result = client.harTilgangTilPerson(Fnr.fromString("01010199999"), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.Uventet>()
    }

    @Test
    fun `harTilgangTilPersoner gir ForMangeIdenter for 413`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
        fakeTransport.leggIKøStatus(statusCode = 413, body = "For mange identer", contentType = "text/plain")

        val result = client.harTilgangTilPersoner(listOf(Fnr.fromString("01010199999")), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") }) shouldBe
            TilgangskontrollFeil.ForMangeIdenter
    }
}
