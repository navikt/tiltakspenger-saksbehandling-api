package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
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
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/saksnummer",
            jwt = jwt,
            forventet = ForventetRespons(status = 200),
            body = """{"fnr":"${fnr.verdi}"}""",
        ).apply {
            val bodyAsText = this.body
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
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Saksnummer? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PUT,
            SAK_PATH,
            jwt = jwt,
            forventet = forventet,
            body = """{"fnr":"${fnr.verdi}"}""",
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) {
                return null
            }
            return Saksnummer(
                JSONObject(bodyAsText).getString(
                    "saksnummer",
                ),
            )
        }
    }
}
