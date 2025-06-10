package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class TaOgOvertaBehandlingTest {
    @Test
    fun `saksbehandler kan overta behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = startSøknadsbehandling(tac)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                }
                overtaBehanding(tac, sak.id, behandlingId, "Z12345", ObjectMother.saksbehandler123()).also {
                    JSONObject(it).getString("saksbehandler") shouldBe "123"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                        it.saksbehandler shouldBe "123"
                    }
                }
                overtaBehanding(tac, sak.id, behandlingId, "123", ObjectMother.saksbehandler()).also {
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
    fun `beslutter kan ta og overta behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                }
                taBehanding(tac, sak.id, behandlingId, ObjectMother.beslutter()).also {
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B12345"
                    }
                }
                overtaBehanding(tac, sak.id, behandlingId, "B12345", ObjectMother.beslutter("B123")).also {
                    JSONObject(it).getString("beslutter") shouldBe "B123"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B123"
                    }
                }

                overtaBehanding(tac, sak.id, behandlingId, "B123", ObjectMother.beslutter()).also {
                    JSONObject(it).getString("beslutter") shouldBe "B12345"
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B12345"
                    }
                }
            }
        }
    }
}
