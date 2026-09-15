package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.søkFnrSaksnummerOgSakIdRoute
import org.junit.jupiter.api.Test

class HentSakRouteTest {

    /**
     * Veileder er en leserolle med personinnsyn og skal kunne søke opp en sak på fnr, sakId eller saksnummer.
     */
    @Test
    fun `veileder kan søke opp sak på fnr`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val sakJson = søkFnrSaksnummerOgSakIdRoute(
                tac = tac,
                id = sak.fnr.verdi,
                saksbehandler = ObjectMother.veileder(),
            )
            sakJson shouldNotBe null
            sakJson!!.getString("saksnummer") shouldBe sak.saksnummer.verdi
        }
    }

    /**
     * Brukere uten rolle som gir personinnsyn (i dag UTVIKLER) skal ikke kunne søke opp sak på fnr.
     * Se [no.nav.tiltakspenger.saksbehandling.sak.infra.routes.søkFnrSaksnummerOgSakIdRoute]
     */
    @Test
    fun `bruker uten personinnsyn kan ikke søke opp sak på fnr`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val sakJson = søkFnrSaksnummerOgSakIdRoute(
                tac = tac,
                id = sak.fnr.verdi,
                saksbehandler = ObjectMother.utvikler(),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
            sakJson shouldBe null
        }
    }
}
