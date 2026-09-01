package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.søkFnrSaksnummerOgSakIdRoute
import org.junit.jupiter.api.Test

class HentSakRouteTest {

    /**
     * Veileder og utvikler er leseroller og skal kunne søke opp en sak på fnr, sakId eller saksnummer.
     */
    @Test
    fun `veileder og utvikler kan søke opp sak på fnr`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                val sakJson = søkFnrSaksnummerOgSakIdRoute(
                    tac = tac,
                    id = sak.fnr.verdi,
                    saksbehandler = leserolle,
                )
                sakJson shouldNotBe null
                sakJson!!.getString("saksnummer") shouldBe sak.saksnummer.verdi
            }
        }
    }
}
