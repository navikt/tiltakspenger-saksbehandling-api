package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import org.junit.jupiter.api.Test

class HentSakForSaksnummerRouteTest {

    /**
     * Veileder og utvikler er leseroller og skal kunne hente en sak på saksnummer.
     */
    @Test
    fun `veileder og utvikler kan hente sak på saksnummer`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                val sakJson = hentSakForSaksnummer(
                    tac = tac,
                    saksnummer = sak.saksnummer,
                    saksbehandler = leserolle,
                )
                sakJson shouldNotBe null
                sakJson!!.getString("saksnummer") shouldBe sak.saksnummer.verdi
            }
        }
    }
}
