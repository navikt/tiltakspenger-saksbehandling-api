package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import org.junit.jupiter.api.Test

class TaTilbakekrevingBehandlingRouteTest {

    @Test
    fun `saksbehandler kan ta tilbakekrevingbehandling med status TIL_BEHANDLING`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar")

            val (oppdatertSak, oppdatertBehandling, json) = tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
            )!!

            oppdatertBehandling.saksbehandler shouldBe "saksbehandlerSomTar"
            oppdatertBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatertBehandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING

            val tilbakekrevingJson = json.get("tilbakekrevinger").first()
            tilbakekrevingJson.get("saksbehandler").toString() shouldBe "\"saksbehandlerSomTar\""
            tilbakekrevingJson.get("status").toString() shouldBe "\"UNDER_BEHANDLING\""
        }
    }

    @Test
    fun `kan ikke ta behandling som allerede er tatt - returnerer 500`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler1 = ObjectMother.saksbehandler("saksbehandler1")
            val saksbehandler2 = ObjectMother.saksbehandler("saksbehandler2")

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler1,
            )!!

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler2,
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke ta behandling med ugyldig tilbakekrevingId - returnerer 500`() {
        withTestApplicationContext { tac ->
            val (sak, _) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = TilbakekrevingId.random(),
                saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke ta behandling med ugyldig sakId - returnerer 500`() {
        withTestApplicationContext { tac ->
            val (_, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = SakId.random(),
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }
}
