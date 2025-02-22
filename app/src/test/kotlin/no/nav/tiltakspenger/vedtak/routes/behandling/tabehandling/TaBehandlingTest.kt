package no.nav.tiltakspenger.vedtak.routes.behandling.tabehandling

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.sendFørstegangsbehandlingTilBeslutning
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.taBehanding
import no.nav.tiltakspenger.vedtak.routes.routes
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class TaBehandlingTest {
    @Test
    fun `saksbehandler kan ta behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (_, _, behandling) = startBehandling(tac)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                }
                taBehanding(tac, behandlingId, ObjectMother.saksbehandler123()).also {
                    JSONObject(it).getString("saksbehandler") shouldBe "123"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                        it.saksbehandler shouldBe "123"
                    }
                }
                taBehanding(tac, behandlingId, ObjectMother.saksbehandler()).also {
                    JSONObject(it).getString("saksbehandler") shouldBe "Z12345"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                        it.saksbehandler shouldBe "Z12345"
                    }
                }
            }
        }
    }

    @Test
    fun `beslutter kan ta behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (_, _, behandlingId) = sendFørstegangsbehandlingTilBeslutning(tac)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                }
                taBehanding(tac, behandlingId, ObjectMother.beslutter()).also {
                    JSONObject(it).getString("beslutter") shouldBe "B12345"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B12345"
                    }
                }

                taBehanding(tac, behandlingId, ObjectMother.beslutter(navIdent = "B123")).also {
                    JSONObject(it).getString("beslutter") shouldBe "B123"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B123"
                    }
                }
            }
        }
    }
}
