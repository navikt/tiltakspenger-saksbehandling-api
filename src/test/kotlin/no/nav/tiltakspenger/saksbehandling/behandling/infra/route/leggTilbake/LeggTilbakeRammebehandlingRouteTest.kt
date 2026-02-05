package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.leggTilbake

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
class LeggTilbakeRammebehandlingRouteTest {

    @Test
    fun `saksbehandler kan legge tilbake behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingId = behandling.id
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandlingId,
                ObjectMother.saksbehandler(),
            ).also { (_, _, jsonBody) ->
                JSONObject(jsonBody).getString("saksbehandler") shouldBe "null"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                    it.saksbehandler shouldBe null
                }
            }
        }
    }

    @Test
    fun `beslutter kan legge tilbake behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            }
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter()).also {
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }
            leggTilbakeRammebehandling(tac, sak.id, behandlingId, ObjectMother.beslutter()).also { (_, _, jsonBody) ->
                JSONObject(jsonBody).getString("beslutter") shouldBe "null"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                    it.beslutter shouldBe null
                }
            }
        }
    }
}
