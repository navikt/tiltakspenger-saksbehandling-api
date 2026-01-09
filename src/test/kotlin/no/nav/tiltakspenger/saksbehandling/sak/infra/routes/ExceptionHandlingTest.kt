package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
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
                    forventetJsonBody = """
                        {
                          "melding": "Noe gikk galt på serversiden",
                          "kode": "server_feil"
                        }
                    """.trimIndent(),
                    forventetStatus = HttpStatusCode.InternalServerError,
                )
            }
        }
    }
}
