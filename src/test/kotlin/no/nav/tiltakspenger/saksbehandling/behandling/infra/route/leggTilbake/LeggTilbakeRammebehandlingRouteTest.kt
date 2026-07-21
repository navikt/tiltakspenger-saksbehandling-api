package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.leggTilbake

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
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
            )!!.also { (_, _, sakJson) ->
                val behandlingJson = sakJson.get("behandlinger").single { it.get("id").asText() == behandlingId.toString() }
                behandlingJson.get("saksbehandler").isNull shouldBe true
                behandlingJson.get("status").asText() shouldBe "KLAR_TIL_BEHANDLING"
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
            leggTilbakeRammebehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())!!.also { (_, _, sakJson) ->
                val behandlingJson = sakJson.get("behandlinger").single { it.get("id").asText() == behandlingId.toString() }
                behandlingJson.get("beslutter").isNull shouldBe true
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                    it.beslutter shouldBe null
                }
            }
        }
    }

    @Test
    fun `kan ikke legge tilbake behandling som er klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandlingId,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetBody = """
                    {
                        "melding": "Kan ikke legge tilbake behandling med status KLAR_TIL_BESLUTNING.",
                        "kode": "ugyldig_status_for_legg_tilbake"
                    }
                """,
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        }
    }

    @Test
    fun `kan ikke legge tilbake behandling som ikke er påbegynt`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            leggTilbakeRammebehandling(tac, sak.id, behandling.id)

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetBody = """
                    {
                        "melding": "Kan ikke legge tilbake behandling med status KLAR_TIL_BEHANDLING.",
                        "kode": "ugyldig_status_for_legg_tilbake"
                    }
                """,
            ) shouldBe null
        }
    }

    @Test
    fun `annen saksbehandler kan ikke legge tilbake behandlingen`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandling.id,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999999"),
                forventetStatus = HttpStatusCode.Forbidden,
                forventetBody = """
                    {
                        "melding": "Du må være saksbehandleren som er tildelt behandlingen for å legge den tilbake.",
                        "kode": "maa_vaere_saksbehandler_for_behandlingen"
                    }
                """,
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `beslutter kan ikke legge tilbake behandling som er under behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandling.id,
                saksbehandler = ObjectMother.beslutter(),
                forventetStatus = HttpStatusCode.Forbidden,
                forventetBody = """
                    {
                        "melding": "Du må være saksbehandler for å legge tilbake denne behandlingen.",
                        "kode": "maa_vaere_saksbehandler"
                    }
                """,
            ) shouldBe null
        }
    }

    @Test
    fun `saksbehandler kan ikke legge tilbake behandling som er under beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandlingId,
                forventetStatus = HttpStatusCode.Forbidden,
                forventetBody = """
                    {
                        "melding": "Du må være beslutter for å legge tilbake denne behandlingen.",
                        "kode": "maa_vaere_beslutter"
                    }
                """,
            ) shouldBe null
        }
    }

    @Test
    fun `annen beslutter kan ikke legge tilbake behandling som er under beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandlingId,
                saksbehandler = ObjectMother.beslutter(navIdent = "B99999"),
                forventetStatus = HttpStatusCode.Forbidden,
                forventetBody = """
                    {
                        "melding": "Du må være beslutteren som er tildelt behandlingen for å legge den tilbake.",
                        "kode": "maa_vaere_beslutter_for_behandlingen"
                    }
                """,
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                it.beslutter shouldBe "B12345"
            }
        }
    }
}
