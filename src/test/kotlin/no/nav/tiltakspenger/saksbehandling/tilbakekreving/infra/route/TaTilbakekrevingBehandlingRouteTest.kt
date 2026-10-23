package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilForhåndsvarsel
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
            val saksbehandler = ObjectMother.tilbakekrevingSaksbehandler("saksbehandlerSomTar")

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
    fun `saksbehandler kan ta tilbakekrevingbehandling med status TIL_FORHÅNDSVARSEL`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilForhåndsvarsel(tac = tac)

            val (_, oppdatertBehandling, json) = tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.tilbakekrevingSaksbehandler("saksbehandlerSomTar"),
            )!!

            oppdatertBehandling.saksbehandler shouldBe "saksbehandlerSomTar"
            oppdatertBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL
            oppdatertBehandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_FORHÅNDSVARSLING

            val tilbakekrevingJson = json.get("tilbakekrevinger").first()
            tilbakekrevingJson.get("saksbehandler").toString() shouldBe "\"saksbehandlerSomTar\""
            tilbakekrevingJson.get("status").toString() shouldBe "\"UNDER_FORHÅNDSVARSLING\""
        }
    }

    @Test
    fun `kan ikke ta behandling som allerede er tatt - returnerer 500`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val saksbehandler1 = ObjectMother.tilbakekrevingSaksbehandler("saksbehandler1")
            val saksbehandler2 = ObjectMother.tilbakekrevingSaksbehandler("saksbehandler2")

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
                saksbehandler = ObjectMother.tilbakekrevingSaksbehandler("saksbehandler"),
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
                saksbehandler = ObjectMother.tilbakekrevingSaksbehandler("saksbehandler"),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `beslutter kan ta tilbakekrevingbehandling med status TIL_GODKJENNING`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)
            val beslutter = ObjectMother.tilbakekrevingBeslutter("beslutterSomTar")

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
                saksbehandler = ObjectMother.tilbakekrevingBeslutter("beslutter1"),
            )!!

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.tilbakekrevingBeslutter("beslutter2"),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `saksbehandler eller beslutter uten tilbakekrevingsrollen kan ikke ta behandlingen - returnerer 403`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakTilBehandling, tilBehandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sakTilGodkjenning, tilGodkjenning) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sakTilBehandling.id,
                tilbakekrevingId = tilBehandling.id,
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerUtenRolle"),
                forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sakTilGodkjenning.id,
                tilbakekrevingId = tilGodkjenning.id,
                saksbehandler = ObjectMother.beslutter("beslutterUtenRolle"),
                forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null

            tac.sakContext.sakRepo.hentForSakId(sakTilBehandling.id)!!
                .tilbakekrevinger.single().saksbehandler shouldBe null
            tac.sakContext.sakRepo.hentForSakId(sakTilGodkjenning.id)!!
                .tilbakekrevinger.single().beslutter shouldBe null
        }
    }

    @Test
    fun `saksbehandler uten tilbakekrevingsrollen ser tilbakekrevingen med lenke, men uten kommandoer`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            val utenRolle = hentSakForSaksnummer(
                tac = tac,
                saksnummer = sak.saksnummer,
                saksbehandler = ObjectMother.saksbehandler(),
            )!!.getJSONArray("tilbakekrevinger").getJSONObject(0)
            utenRolle.getString("id") shouldBe behandling.id.toString()
            utenRolle.getString("url") shouldBe behandling.url
            utenRolle.getJSONArray("gyldigeKommandoer").length() shouldBe 0

            val medRolle = hentSakForSaksnummer(
                tac = tac,
                saksnummer = sak.saksnummer,
                saksbehandler = ObjectMother.tilbakekrevingSaksbehandler(),
            )!!.getJSONArray("tilbakekrevinger").getJSONObject(0)
            medRolle.getString("url") shouldBe behandling.url
            medRolle.getJSONArray("gyldigeKommandoer").length() shouldBe 1
        }
    }
}
