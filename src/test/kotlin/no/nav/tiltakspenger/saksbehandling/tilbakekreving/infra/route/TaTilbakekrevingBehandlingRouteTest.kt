package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilGodkjenning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import org.junit.jupiter.api.Test

class TaTilbakekrevingBehandlingRouteTest {

    @Test
    fun `saksbehandler kan ta tilbakekrevingbehandling med status TIL_BEHANDLING`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar")

            val (_, oppdatertBehandling, json) = tildelTilbakekrevingBehandling(
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

            val åpenTilbakekrevingJson = json.get("åpneBehandlinger").single {
                it.get("type").asString() == "TILBAKEKREVING"
            }
            åpenTilbakekrevingJson.get("id").asString() shouldBe behandling.id.toString()
        }
    }

    @Test
    fun `kan ikke ta behandling som allerede er tatt - returnerer 500`() {
        withTestApplicationContextAndPostgres { tac ->
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
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke ta behandling med ugyldig tilbakekrevingId - returnerer 500`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = TilbakekrevingId.random(),
                saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke ta behandling med ugyldig sakId - returnerer 500`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = SakId.random(),
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `beslutter kan ta tilbakekrevingbehandling med status TIL_GODKJENNING`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)
            val beslutter = ObjectMother.beslutter("beslutterSomTar")

            val (_, oppdatertBehandling) = tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = beslutter,
            )!!

            oppdatertBehandling.beslutter shouldBe "beslutterSomTar"
            oppdatertBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
            oppdatertBehandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING
        }
    }

    @Test
    fun `kan ikke ta behandling til godkjenning som allerede er tatt av en beslutter - returnerer 500`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.beslutter("beslutter1"),
            )!!

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.beslutter("beslutter2"),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }
}
