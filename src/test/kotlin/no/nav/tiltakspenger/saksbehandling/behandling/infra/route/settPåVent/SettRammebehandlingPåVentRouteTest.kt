package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.settPåVent

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.KanIkkeSetteRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldHaSisteVentestatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettRammebehandlingPåVentRouteTest {
    @Test
    fun `sett søknadsbehandling på vent`() {
        withTestApplicationContext { tac ->
            val (_, _, søknadsbehandling, sakJson) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            søknadsbehandling!!.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            søknadsbehandling.saksbehandler shouldBe null
            søknadsbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "Z12345",
                            begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = LocalDate.parse("2026-01-01"),
                        ),
                    ),
                ),
            )
            sakJson.rammebehandlingJson(søknadsbehandling.id).also { behandlingJson ->
                behandlingJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                behandlingJson.get("saksbehandler").isNull shouldBe true
                behandlingJson.shouldHaSisteVentestatus(
                    sattPåVentAv = "Z12345",
                    begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                    status = "UNDER_BEHANDLING",
                    frist = LocalDate.parse("2026-01-01"),
                    forventetAntallHendelser = 1,
                )
            }
        }
    }

    @Test
    fun `kan sette søknadsbehandling under beslutning på vent`() {
        withTestApplicationContext { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
                it.beslutter shouldBe null
                it.ventestatus.ventestatusHendelser.last().let { hendelse ->
                    hendelse.endretAv shouldBe beslutter.navIdent
                    hendelse.erSattPåVent shouldBe true
                    hendelse.status shouldBe "UNDER_BESLUTNING"
                }
            }
        }
    }

    @Test
    fun `beslutter kan ikke overskrive angring ved å sette rammebehandling på vent`() {
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
            val service = SettRammebehandlingPåVentService(behandlingService, tac.clock)

            service.settBehandlingPåVent(
                SettRammebehandlingPåVentKommando(
                    sakId = sak.id,
                    rammebehandlingId = behandlingId,
                    oppdaterPåVent = false,
                    begrunnelse = "Beslutter setter på vent",
                    saksbehandler = beslutter,
                    frist = null,
                ),
            ) shouldBe KanIkkeSetteRammebehandlingPåVent.BehandlingenErIkkeLengerUnderBeslutning.left()

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId) shouldBe angretBehandling
            verify(exactly = 0) { statistikkService.lagre(any<StatistikkDTO>(), any()) }
        }
    }

    @Test
    fun `kan ikke sette søknadsbehandling som er klar til behandling på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
                forventet = ugyldigStatus("KLAR_TIL_BEHANDLING"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sette søknadsbehandling som er klar til beslutning på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                forventet = ugyldigStatus("KLAR_TIL_BESLUTNING"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sette vedtatt søknadsbehandling på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtak, _) = iverksettSøknadsbehandling(tac)

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammevedtak.behandlingId,
                forventet = ugyldigStatus("VEDTATT"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sette avbrutt søknadsbehandling på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, _) = opprettSøknadsbehandlingOgAvbryt(tac)!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = søknadsbehandling!!.id,
                forventet = ugyldigStatus("AVBRUTT"),
            ) shouldBe null
        }
    }

    @Test
    fun `en annen saksbehandler enn den som er tildelt behandlingen kan ikke sette den på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandleren som er tildelt behandlingen for å sette den på vent.",
                      "kode": "maa_vaere_saksbehandler_for_behandlingen"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).ventestatus.erSattPåVent shouldBe false
        }
    }

    @Test
    fun `en bruker uten saksbehandlerrolle kan ikke sette en behandling under behandling på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandler for å sette denne behandlingen på vent.",
                      "kode": "maa_vaere_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).ventestatus.erSattPåVent shouldBe false
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke sette en behandling under beslutning på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), ObjectMother.beslutter())!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å sette denne behandlingen på vent.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).ventestatus.erSattPåVent shouldBe false
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på en søknadsbehandling som er klar til behandling og satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingPåVent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            val annenSaksbehandler = ObjectMother.saksbehandler123()

            val (_, _, oppdatertBehandling, sakJson) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingPåVent!!.id,
                saksbehandler = annenSaksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 15.januar(2026),
                oppdaterSattPåVent = true,
            )!!

            oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertBehandling.saksbehandler shouldBe null
            oppdatertBehandling.beslutter shouldBe null
            oppdatertBehandling.ventestatus.erSattPåVent shouldBe true
            oppdatertBehandling.ventestatus.ventestatusHendelser.size shouldBe 2
            oppdatertBehandling.ventestatus.ventestatusHendelser.last().let { hendelse ->
                hendelse.endretAv shouldBe annenSaksbehandler.navIdent
                hendelse.begrunnelse shouldBe "Oppdatert begrunnelse"
                hendelse.frist shouldBe 15.januar(2026)
                hendelse.erSattPåVent shouldBe true
                hendelse.status shouldBe "KLAR_TIL_BEHANDLING"
            }
            sakJson.rammebehandlingJson(oppdatertBehandling.id).also { behandlingJson ->
                behandlingJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                behandlingJson.get("saksbehandler").isNull shouldBe true
                behandlingJson.shouldHaSisteVentestatus(
                    sattPåVentAv = annenSaksbehandler.navIdent,
                    begrunnelse = "Oppdatert begrunnelse",
                    status = "KLAR_TIL_BEHANDLING",
                    frist = 15.januar(2026),
                    forventetAntallHendelser = 2,
                )
            }
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på en søknadsbehandling som er klar til beslutning og satt på vent`() {
        withTestApplicationContext { tac ->
            val beslutter = ObjectMother.beslutter()
            val annenBeslutter = ObjectMother.beslutter(navIdent = "B99999")
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!
            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = annenBeslutter,
                begrunnelse = "Oppdatert begrunnelse fra beslutter",
                frist = 15.januar(2026),
                oppdaterSattPåVent = true,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
                it.beslutter shouldBe null
                it.ventestatus.ventestatusHendelser.size shouldBe 2
                it.ventestatus.ventestatusHendelser.last().let { hendelse ->
                    hendelse.endretAv shouldBe annenBeslutter.navIdent
                    hendelse.begrunnelse shouldBe "Oppdatert begrunnelse fra beslutter"
                    hendelse.frist shouldBe 15.januar(2026)
                    hendelse.erSattPåVent shouldBe true
                    hendelse.status shouldBe "KLAR_TIL_BESLUTNING"
                }
            }
        }
    }

    @Test
    fun `kan ikke oppdatere en behandling som ikke er satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
                oppdaterSattPåVent = true,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen må være satt på vent for å kunne oppdateres.",
                      "kode": "må_være_satt_på_vent"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.ventestatus.erSattPåVent shouldBe false
            }
        }
    }

    @Test
    fun `kan ikke sette en behandling på vent på nytt uten å oppdatere`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingPåVent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingPåVent!!.id,
                oppdaterSattPåVent = false,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen er allerede satt på vent.",
                      "kode": "behandlingen_er_allerede_paa_vent"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingPåVent.id).ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }

    @Test
    fun `en bruker uten saksbehandlerrolle kan ikke oppdatere en behandling som er klar til behandling og satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingPåVent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingPåVent!!.id,
                saksbehandler = ObjectMother.beslutter(),
                oppdaterSattPåVent = true,
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandler for å sette denne behandlingen på vent.",
                      "kode": "maa_vaere_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingPåVent.id).ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke oppdatere en behandling som er klar til beslutning og satt på vent`() {
        withTestApplicationContext { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!
            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = ObjectMother.saksbehandler(),
                oppdaterSattPåVent = true,
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å sette denne behandlingen på vent.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }

    private fun ugyldigStatus(status: String): ForventetRespons = ForventetRespons.json(
        400,
        """
        {
          "melding": "Kan ikke sette behandling med status $status på vent.",
          "kode": "ugyldig_status_for_sett_paa_vent"
        }
        """.trimIndent(),
        "application/json; charset=UTF-8",
    )
}
