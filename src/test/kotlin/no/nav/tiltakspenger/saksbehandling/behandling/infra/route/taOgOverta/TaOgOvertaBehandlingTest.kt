package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class TaOgOvertaBehandlingTest {
    @Test
    fun `saksbehandler kan overta behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingId = behandling.id
            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "Z12345", ObjectMother.saksbehandler123()).also { jsonBody ->
                JSONObject(jsonBody).getString("saksbehandler") shouldBe "123"
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "123"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "123", ObjectMother.saksbehandler()).also { jsonBody ->
                JSONObject(jsonBody).getString("saksbehandler") shouldBe "Z12345"
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                }
            }
        }
    }

    @Test
    fun `beslutter kan ta og overta behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            }
            tac.clock.spol1timeFrem()
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter()).also {
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "B12345", ObjectMother.beslutter("B123")).also { jsonBody ->
                JSONObject(jsonBody).getString("beslutter") shouldBe "B123"
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B123"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "B123", ObjectMother.beslutter()).also { jsonBody ->
                JSONObject(jsonBody).getString("beslutter") shouldBe "B12345"
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }
        }
    }
}
