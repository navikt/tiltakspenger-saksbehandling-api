package no.nav.tiltakspenger.saksbehandling.person.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

class HentPersonopplysningerRouteTest {

    /**
     * Veileder og utvikler er leseroller og skal kunne hente personopplysninger for en sak.
     */
    @Test
    fun `veileder og utvikler kan hente personopplysninger for en sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = leserolle)
                tac.leggTilBruker(jwt, leserolle)
                defaultRequestWithAssertions(
                    HttpMethod.GET,
                    "/sak/${sak.id}/personopplysninger",
                    jwt = jwt,
                    forventet = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
                ).apply {
                    JSONObject(body).getString("fnr") shouldBe sak.fnr.verdi
                }
            }
        }
    }
}
