package no.nav.tiltakspenger.vedtak.routes.behandling.sendtilbake

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.sendTilbake
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

class SendTilbakeTilSaksbehandlerTest {

    @Test
    fun `sjekk at begrunnelse kan sendes inn`() {
        runTest {
            with(TestApplicationContext()) {
                val tac = this
                testApplication {
                    application {
                        jacksonSerialization()
                        routing { routes(tac) }
                    }
                    val (_, _, behandlingId, _) = this.sendTilbake(tac)
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).attesteringer.single().let {
                        it shouldBe
                            Attestering(
                                // Ignorerer id+tidspunkt
                                id = it.id,
                                tidspunkt = it.tidspunkt,
                                status = no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus.SENDT_TILBAKE,
                                begrunnelse = "send_tilbake_begrunnelse",
                                beslutter = ObjectMother.beslutter().navIdent,
                            )
                    }
                }
            }
        }
    }
}
