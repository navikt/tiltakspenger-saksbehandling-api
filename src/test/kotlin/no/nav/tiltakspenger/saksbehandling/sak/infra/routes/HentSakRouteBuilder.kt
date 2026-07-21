package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
    ): JSONObject? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { ForventetRespons(status = it) },
        ) { setBody("""{"fnr":"$id"}""") }.apply {
            val bodyAsText = this.bodyAsText()
            if (status != HttpStatusCode.OK) return null
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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
    ): JSONObject? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.Get,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/${saksnummer.verdi}")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { ForventetRespons(status = it) },
        ).apply {
            val bodyAsText = this.bodyAsText()
            if (status != HttpStatusCode.OK) return null
            return JSONObject(bodyAsText)
        }
    }
}
