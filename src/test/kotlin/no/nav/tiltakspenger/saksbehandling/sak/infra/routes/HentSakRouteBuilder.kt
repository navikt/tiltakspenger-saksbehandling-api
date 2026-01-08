package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak")
            },
            jwt = jwt,
        ) { setBody("""{"fnr":"$id"}""") }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
            }
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
        defaultRequest(
            HttpMethod.Get,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/${saksnummer.verdi}")
            },
            jwt = jwt,
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
            }
            if (status != HttpStatusCode.OK) return null
            return JSONObject(bodyAsText)
        }
    }
}
