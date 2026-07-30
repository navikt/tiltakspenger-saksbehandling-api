package no.nav.tiltakspenger.saksbehandling.person.infra.routes

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.SAKSBEHANDLER_PATH
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class MeRouteTest {
    // language = JSON
    private val saksbehandlerMock =
        """
        {
          "navIdent":"Z12345",
          "brukernavn":"Sak Behandler",
          "epost":"Sak.Behandler@nav.no",
          "roller":["SAKSBEHANDLER"]
        }
        """.trimIndent()

    @Test
    fun `get saksbehandler - er saksbehandler med gyldig token - returnerer saksbehandler`() {
        val texasClient = mockk<TexasClient>()
        coEvery { texasClient.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
            roles = null,
            other = mutableMapOf(
                "azp_name" to "saksbehandling",
                "azp" to "saksbehandling-id",
                "NAVident" to "Z12345",
                "preferred_username" to "Sak.Behandler@nav.no",
            ),
        )
        runTest {
            withTestApplicationContext(texasClient = texasClient) {
                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    SAKSBEHANDLER_PATH,
                    forventet = ForventetRespons(
                        status = 200,
                        contentType = "application/json; charset=UTF-8",
                    ),
                ).apply {
                    JSONAssert.assertEquals(
                        saksbehandlerMock,
                        body,
                        JSONCompareMode.LENIENT,
                    )
                }
            }
        }
    }

    @Test
    fun `get saksbehandler - utløpt token - returnerer 401`() {
        val texasClient = mockk<TexasClient>()
        coEvery { texasClient.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
            active = false,
            error = null,
            groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
            roles = null,
            other = mutableMapOf(
                "azp_name" to "saksbehandling",
                "azp" to "saksbehandling-id",
                "NAVident" to "Z12345",
                "preferred_username" to "Sak.Behandler@nav.no",
            ),
        )
        runTest {
            withTestApplicationContext(texasClient = texasClient) {
                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    SAKSBEHANDLER_PATH,
                    forventet = ForventetRespons(status = 401),
                )
            }
        }
    }

    @Test
    fun `get saksbehandler - ugyldig token - returnerer 403`() {
        val texasClient = mockk<TexasClient>()
        coEvery { texasClient.introspectToken(any(), IdentityProvider.AZUREAD) } returns TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
            roles = null,
            other = mutableMapOf(
                "azp_name" to "saksbehandling",
                "azp" to "saksbehandling-id",
            ),
        )
        runTest {
            withTestApplicationContext(texasClient = texasClient) {
                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    SAKSBEHANDLER_PATH,
                    forventet = ForventetRespons(status = 403),
                )
            }
        }
    }
}
