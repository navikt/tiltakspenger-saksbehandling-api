package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import org.junit.jupiter.api.Test

class LeggTilbakeTilbakekrevingBehandlingRouteTest {

    @Test
    fun `saksbehandler kan legge tilbake behandling hen selv har tatt`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler1")

            taTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
            )!!

            val (_, oppdatertBehandling, json) = leggTilbakeTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
            )!!

            oppdatertBehandling.saksbehandler shouldBe null
            oppdatertBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatertBehandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING

            val tilbakekrevingJson = json.get("tilbakekrevinger").first()
            tilbakekrevingJson.get("saksbehandler").toString() shouldBe "null"
            tilbakekrevingJson.get("status").toString() shouldBe "\"TIL_BEHANDLING\""
        }
    }

    @Test
    fun `kan ikke legge tilbake behandling som en annen saksbehandler eier - returnerer 500`() {
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

            leggTilbakeTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler2,
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke legge tilbake behandling som ikke er tatt - returnerer 500`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler1")

            leggTilbakeTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler,
                forventetStatus = HttpStatusCode.InternalServerError,
            ) shouldBe null
        }
    }

    @Test
    fun `ta-leggTilbake-ta roundtrip bevarer ekstern status`() {
        withTestApplicationContext { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler1 = ObjectMother.saksbehandler("saksbehandler1")
            val saksbehandler2 = ObjectMother.saksbehandler("saksbehandler2")

            // Saksbehandler 1 tar behandlingen
            val (_, etterTa, _) = taTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler1,
            )!!
            etterTa.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
            etterTa.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING

            // Saksbehandler 1 legger tilbake
            val (_, etterLeggTilbake, _) = leggTilbakeTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler1,
            )!!
            etterLeggTilbake.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING
            etterLeggTilbake.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            etterLeggTilbake.saksbehandler shouldBe null

            // Saksbehandler 2 tar behandlingen
            val (_, etterNyTa, _) = taTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = saksbehandler2,
            )!!
            etterNyTa.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
            etterNyTa.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            etterNyTa.saksbehandler shouldBe "saksbehandler2"
        }
    }
}
