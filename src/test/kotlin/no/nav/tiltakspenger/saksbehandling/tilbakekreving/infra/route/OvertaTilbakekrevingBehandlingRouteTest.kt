package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import org.junit.jupiter.api.Test

class OvertaTilbakekrevingBehandlingRouteTest {

    @Test
    fun `saksbehandler kan overta behandling fra en annen saksbehandler`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler1 = ObjectMother.saksbehandler("saksbehandler1")
            val saksbehandler2 = ObjectMother.saksbehandler("saksbehandler2")

            taTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler1,
            )!!

            val (oppdatertSak, oppdatertBehandling, json) = overtaTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler2,
            )!!

            oppdatertBehandling.saksbehandlerIdent shouldBe "saksbehandler2"
            oppdatertBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatertBehandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING

            val tilbakekrevingJson = json.get("tilbakekrevinger").first()
            tilbakekrevingJson.get("saksbehandler").toString() shouldBe "\"saksbehandler2\""
            tilbakekrevingJson.get("status").toString() shouldBe "\"UNDER_BEHANDLING\""
        }
    }

    @Test
    fun `kan ikke overta fra seg selv - returnerer 500`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler1")

            taTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
            )!!

            overtaTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke overta behandling som ikke er tatt - returnerer 500`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar")

            overtaTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }
}
