package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilForhåndsvarsel
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevinger
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import org.junit.jupiter.api.Test

class TildelTilbakekrevingerRouteTest {

    @Test
    fun `saksbehandler kan ta flere tilbakekrevinger på tvers av saker`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak1, tilbakekreving1) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sak2, tilbakekreving2) = opprettTilbakekrevingBehandlingTilForhåndsvarsel(tac = tac)

            val (oppdaterte, json) = tildelTilbakekrevinger(
                tac = tac,
                tilbakekrevinger = listOf(sak1.id to tilbakekreving1.id, sak2.id to tilbakekreving2.id),
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar"),
            )!!

            oppdaterte.map { it.saksbehandler } shouldBe listOf("saksbehandlerSomTar", "saksbehandlerSomTar")
            oppdaterte.map { it.statusIntern } shouldBe listOf(
                TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING,
                TilbakekrevingBehandlingsstatusIntern.UNDER_FORHÅNDSVARSLING,
            )

            json.get("tilbakekrevinger").toList().map {
                it.get("tilbakekrevingId").asString() to it.get("saksnummer").asString()
            } shouldContainExactlyInAnyOrder listOf(
                tilbakekreving1.id.toString() to sak1.saksnummer.toString(),
                tilbakekreving2.id.toString() to sak2.saksnummer.toString(),
            )
        }
    }

    @Test
    fun `tildeler ingen dersom én av tilbakekrevingene ikke kan tas`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak1, ledig) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sak2, alleredeTatt) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak2.id,
                tilbakekrevingId = alleredeTatt.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
            )!!

            tildelTilbakekrevinger(
                tac = tac,
                tilbakekrevinger = listOf(sak1.id to ledig.id, sak2.id to alleredeTatt.id),
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar"),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null

            val ikkeTildelt = tac.sakContext.sakRepo.hentForSakId(sak1.id)!!.tilbakekrevinger.single()
            ikkeTildelt.saksbehandler shouldBe null
            ikkeTildelt.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING
        }
    }

    @Test
    fun `må sende inn minst én tilbakekreving`() {
        withTestApplicationContextAndPostgres { tac ->
            tildelTilbakekrevinger(
                tac = tac,
                tilbakekrevinger = emptyList(),
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        """{"melding":"Du må sende inn minst én tilbakekreving.","kode":"må_ha_minst_en_tilbakekreving"}""",
                    ),
                ),
            ) shouldBe null
        }
    }
}
