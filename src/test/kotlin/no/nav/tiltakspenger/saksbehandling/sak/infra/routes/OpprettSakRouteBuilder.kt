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
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.json.JSONObject

interface OpprettSakRouteBuilder {
    /**
     * Kalles via systembruker.
     */
    suspend fun ApplicationTestBuilder.hentEllerOpprettSakForSystembruker(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
    ): Saksnummer {
        val jwt = tac.jwtGenerator.createJwtForSystembruker(
            roles = listOf("hent_eller_opprett_sak"),
        )
        tac.leggTilBruker(jwt, ObjectMother.systembrukerHentEllerOpprettSak())
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/saksnummer")
            },
            jwt = jwt,
        ) { setBody("""{"fnr":"${fnr.verdi}"}""") }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return Saksnummer(
                JSONObject(bodyAsText).getString(
                    "saksnummer",
                ),
            )
        }
    }

    /**
     * Kalles via systembruker.
     */
    suspend fun ApplicationTestBuilder.hentEllerOpprettSakForSaksbehandler(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fnr: Fnr = Fnr.random(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Saksnummer? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Put,
            url {
                protocol = URLProtocol.HTTPS
                path(SAK_PATH)
            },
            jwt = jwt,
        ) { setBody("""{"fnr":"${fnr.verdi}"}""") }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
            }
            if (status != HttpStatusCode.OK) {
                return null
            }
            if (forventetJsonBody != null) {
                bodyAsText shouldBe forventetJsonBody
            }
            this.contentType()!! shouldBe "application/json; charset=UTF-8"
            return Saksnummer(
                JSONObject(bodyAsText).getString(
                    "saksnummer",
                ),
            )
        }
    }
}
