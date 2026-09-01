package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

class StartRevurderingRouteTest {

    /**
     * Veileder og utvikler er leseroller og skal ikke kunne opprette en behandling.
     * Rollesjekken skjer før tilgangskontroll og domenelogikk, så feilen er den samme uansett sakens tilstand.
     */
    @Test
    fun `veileder og utvikler får 403 ved opprettelse av revurdering`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(tac)

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = leserolle)
                tac.leggTilBruker(jwt, leserolle)
                defaultRequestWithAssertions(
                    HttpMethod.POST,
                    "/sak/${sak.id}/revurdering/start",
                    jwt = jwt,
                    forventet = ForventetRespons(status = 403, contentType = "application/json; charset=UTF-8"),
                    body = """{"revurderingType": "STANS"}""",
                ).apply {
                    JSONObject(body).apply {
                        getString("kode") shouldBe "tilgang_nektet_krev_rolle"
                        getString("melding") shouldContain "mangler rollen SAKSBEHANDLER"
                    }
                }
            }
        }
    }
}
