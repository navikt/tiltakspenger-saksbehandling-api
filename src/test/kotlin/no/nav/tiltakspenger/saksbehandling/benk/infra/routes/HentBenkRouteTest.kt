package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennForBehandlingId
import org.junit.jupiter.api.Test

class HentBenkRouteTest {

    /**
     * Kjører mot postgres fordi det er benk-spørringen som leser attesteringene og utleder `erUnderkjent`.
     * Benken sveiper over hele det delte skjemaet, så identer-filteret avgrenser responsen til denne testens egen saksbehandler.
     */
    @Test
    fun `underkjent søknadsbehandling vises i benken med erUnderkjent`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999801")
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac, saksbehandler = saksbehandler)
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())
            underkjennForBehandlingId(tac, sak.id, behandlingId)

            val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
            tac.leggTilBruker(jwt, saksbehandler)
            val respons = defaultRequestWithAssertions(
                HttpMethod.POST,
                "/behandlinger",
                jwt = jwt,
                forventet = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
                body = """{"sortering": "STARTET,DESC", "filters": {"benktype": null, "behandlingstype": null, "status": null, "identer": ["Z999801"], "tilbakekrevingKunOverMinstebeløp": false}}""",
            ).body

            val benk = deserialize<TilgangsfiltrertBenkOversiktDTO>(respons)
            benk.totalAntall shouldBe 1
            benk.behandlingssammendrag.single().let {
                it.saksnummer shouldBe sak.saksnummer.verdi
                it.status shouldBe BehandlingssammendragStatusDto.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z999801"
                it.erUnderkjent shouldBe true
            }
        }
    }
}
