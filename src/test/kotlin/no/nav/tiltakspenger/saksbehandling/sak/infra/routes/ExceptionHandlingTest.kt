package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSaksbehandler
import org.junit.jupiter.api.Test

class ExceptionHandlingTest {

    @Test
    fun `IllegalStateException skal bli til 500`() {
        runTest {
            withTestApplicationContext(
                tilgangsmaskinFakeClient = object : TilgangsmaskinFakeTestClient() {
                    override suspend fun harTilgangTilPerson(fnr: Fnr, saksbehandlerToken: String) =
                        throw IllegalStateException("Tvingt feil for testing")
                },
            ) { tac ->
                hentEllerOpprettSakForSaksbehandler(
                    tac = tac,
                    forventet = ForventetRespons.json(
                        500,
                        """
                        {
                          "melding": "Noe gikk galt på serversiden",
                          "kode": "server_feil"
                        }
                        """.trimIndent(),
                        "application/json; charset=UTF-8",
                    ),
                )
            }
        }
    }
}
