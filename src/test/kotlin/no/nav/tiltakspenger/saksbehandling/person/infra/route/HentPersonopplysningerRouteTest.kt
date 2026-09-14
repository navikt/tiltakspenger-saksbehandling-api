package no.nav.tiltakspenger.saksbehandling.person.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

class HentPersonopplysningerRouteTest {

    /**
     * Veileder er en fagrolle og skal se personopplysningene som de er.
     */
    @Test
    fun `veileder kan hente personopplysninger for en sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val veileder = ObjectMother.veileder()

            val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = veileder)
            tac.leggTilBruker(jwt, veileder)
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

    /**
     * Utvikler er en leserolle uten tjenstlig behov for personopplysninger, og får dem sladdet.
     */
    @Test
    fun `utvikler kan hente personopplysninger for en sak, men får dem sladdet`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val utvikler = ObjectMother.utvikler()

            val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = utvikler)
            tac.leggTilBruker(jwt, utvikler)
            defaultRequestWithAssertions(
                HttpMethod.GET,
                "/sak/${sak.id}/personopplysninger",
                jwt = jwt,
                forventet = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
            ).apply {
                JSONObject(body).apply {
                    getString("fnr") shouldBe SLADDET_TEKST
                    getString("fødselsdato") shouldBe SLADDET_TEKST
                    getString("fornavn") shouldBe SLADDET_TEKST
                    getString("etternavn") shouldBe SLADDET_TEKST
                    getBoolean("skjermet") shouldBe false
                }
            }
        }
    }
}
