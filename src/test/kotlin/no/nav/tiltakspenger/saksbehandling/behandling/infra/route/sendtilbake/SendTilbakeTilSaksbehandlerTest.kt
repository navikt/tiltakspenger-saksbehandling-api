package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.sendtilbake

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendTilbake
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
                            no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering(
                                // Ignorerer id+tidspunkt
                                id = it.id,
                                tidspunkt = it.tidspunkt,
                                status = no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus.SENDT_TILBAKE,
                                begrunnelse = NonBlankString.create("send_tilbake_begrunnelse"),
                                beslutter = ObjectMother.beslutter().navIdent,
                            )
                    }
                }
            }
        }
    }
}
