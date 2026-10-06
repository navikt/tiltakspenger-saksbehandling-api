package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.leggTilbake

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.KanIkkeLeggeTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
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
                val behandlingJson = sakJson.get("rammebehandlinger").single { it.get("id").asString() == behandlingId.toString() }
                behandlingJson.get("saksbehandler").isNull shouldBe true
                behandlingJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
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
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), ObjectMother.beslutter()).also {
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }
            leggTilbakeRammebehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())!!.also { (_, _, sakJson) ->
                val behandlingJson = sakJson.get("rammebehandlinger").single { it.get("id").asString() == behandlingId.toString() }
                behandlingJson.get("beslutter").isNull shouldBe true
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                    it.beslutter shouldBe null
                }
            }
        }
    }

    @Test
    fun `beslutter kan ikke overskrive angring ved å legge tilbake behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!
            val sakUnderBeslutning = tac.sakContext.sakService.hentForSakId(sak.id)

            val (_, angretBehandling) = angreRammebehandling(tac, sak.id, behandlingId)!!
            angretBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING

            val sakService = mockk<SakService>()
            every { sakService.hentForSakId(sak.id) } returns sakUnderBeslutning
            val statistikkService = spyk(tac.statistikkContext.statistikkService)
            val behandlingService = RammebehandlingService(
                rammebehandlingRepo = tac.behandlingContext.rammebehandlingRepo,
                sakService = sakService,
                sessionFactory = tac.sessionFactory,
                clock = tac.clock,
                statistikkService = statistikkService,
            )
            val service = LeggTilbakeRammebehandlingService(
                behandlingService = behandlingService,
                rammebehandlingRepo = tac.behandlingContext.rammebehandlingRepo,
                statistikkService = statistikkService,
                sessionFactory = tac.sessionFactory,
                clock = tac.clock,
            )

            service.leggTilbakeRammebehandling(
                sakId = sak.id,
                behandlingId = behandlingId,
                saksbehandler = beslutter,
            ) shouldBe KanIkkeLeggeTilbakeRammebehandling.BehandlingenErIkkeLengerUnderBeslutning.left()

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId) shouldBe angretBehandling
            verify(exactly = 0) { statistikkService.lagre(any<StatistikkDTO>(), any()) }
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
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kan ikke legge tilbake behandling med status KLAR_TIL_BESLUTNING.",
                      "kode": "ugyldig_status_for_legg_tilbake"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
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
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kan ikke legge tilbake behandling med status KLAR_TIL_BEHANDLING.",
                      "kode": "ugyldig_status_for_legg_tilbake"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
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
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandleren som er tildelt behandlingen for å legge den tilbake.",
                      "kode": "maa_vaere_saksbehandler_for_behandlingen"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
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
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandler for å legge tilbake denne behandlingen.",
                      "kode": "maa_vaere_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `saksbehandler kan ikke legge tilbake behandling som er under beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), ObjectMother.beslutter())

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandlingId,
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å legge tilbake denne behandlingen.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `annen beslutter kan ikke legge tilbake behandling som er under beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), ObjectMother.beslutter())

            leggTilbakeRammebehandling(
                tac,
                sak.id,
                behandlingId,
                saksbehandler = ObjectMother.beslutter(navIdent = "B99999"),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutteren som er tildelt behandlingen for å legge den tilbake.",
                      "kode": "maa_vaere_beslutter_for_behandlingen"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                it.beslutter shouldBe "B12345"
            }
        }
    }
}
