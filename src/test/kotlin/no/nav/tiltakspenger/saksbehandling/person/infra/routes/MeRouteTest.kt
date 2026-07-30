package no.nav.tiltakspenger.saksbehandling.person.infra.routes

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.SAKSBEHANDLER_PATH
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

/**
 * Bruker [TexasClientFake] framfor en mock av `TexasClient`.
 * Faken utleder introspeksjonssvaret fra brukeren som er registrert på tokenet, slik Texas gjør i prod, i stedet for at testen dikterer svaret direkte.
 */
class MeRouteTest {
    // language = JSON
    private val forventetSaksbehandler =
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
        runTest {
            withTestApplicationContext { tac ->
                val saksbehandler = ObjectMother.saksbehandler()
                val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
                tac.leggTilBruker(jwt, saksbehandler)

                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    SAKSBEHANDLER_PATH,
                    jwt = jwt,
                    forventet = ForventetRespons(
                        status = 200,
                        contentType = "application/json; charset=UTF-8",
                    ),
                ).apply {
                    JSONAssert.assertEquals(
                        forventetSaksbehandler,
                        body,
                        JSONCompareMode.LENIENT,
                    )
                }
            }
        }
    }

    @Test
    fun `get saksbehandler - ukjent token - returnerer 401`() {
        runTest {
            withTestApplicationContext { tac ->
                // Tokenet registreres bevisst ikke, så faken svarer active = false slik Texas gjør for et utløpt eller ukjent token.
                val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = ObjectMother.saksbehandler())

                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    SAKSBEHANDLER_PATH,
                    jwt = jwt,
                    forventet = ForventetRespons(status = 401),
                )
            }
        }
    }

    @Test
    fun `get saksbehandler - aktivt token uten NAVident - returnerer 403`() {
        // Et aktivt token som mangler NAVident og preferred_username, altså en applikasjon og ikke en saksbehandler.
        // Faken kan ikke uttrykke det fra en Bruker, så vi overstyrer selve svaret her i stedet for å mocke klienten.
        val texasClientUtenNavIdent = object : TexasClientFake(fixedClock) {
            override suspend fun introspectToken(
                token: String,
                identityProvider: IdentityProvider,
            ): TexasIntrospectionResponse = TexasIntrospectionResponse(
                active = true,
                error = null,
                groups = listOf("1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"),
                roles = null,
                other = mutableMapOf(
                    "azp_name" to "saksbehandling",
                    "azp" to "saksbehandling-id",
                ),
            )
        }

        runTest {
            withTestApplicationContext(texasClient = texasClientUtenNavIdent) { tac ->
                val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = ObjectMother.saksbehandler())

                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    SAKSBEHANDLER_PATH,
                    jwt = jwt,
                    forventet = ForventetRespons(status = 403),
                )
            }
        }
    }
}
