package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.json.JSONObject

/**
 * Se [no.nav.tiltakspenger.saksbehandling.sak.infra.routes.søkFnrSaksnummerOgSakIdRoute]
 * Se [no.nav.tiltakspenger.saksbehandling.sak.infra.routes.hentSakForSaksnummerRoute]
 */
interface HentSakRouteBuilder {
    /**
     * Se [no.nav.tiltakspenger.saksbehandling.sak.infra.routes.søkFnrSaksnummerOgSakIdRoute]
     * @param id kan være fnr, sakId eller saksnummer
     * @return Serialisert [SakDTO] eller null dersom status ikke er OK
     */
    suspend fun ApplicationTestBuilder.søkFnrSaksnummerOgSakIdRoute(
        tac: TestApplicationContext,
        id: String,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): JSONObject? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak",
            jwt = jwt,
            forventet = forventet,
            body = """{"fnr":"$id"}""",
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) return null
            return JSONObject(bodyAsText)
        }
    }

    /**
     * @return Serialisert [SakDTO] eller null dersom status ikke er OK
     */
    suspend fun ApplicationTestBuilder.hentSakForSaksnummer(
        tac: TestApplicationContext,
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): JSONObject? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.GET,
            "/sak/${saksnummer.verdi}",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) return null
            return JSONObject(bodyAsText)
        }
    }
}
