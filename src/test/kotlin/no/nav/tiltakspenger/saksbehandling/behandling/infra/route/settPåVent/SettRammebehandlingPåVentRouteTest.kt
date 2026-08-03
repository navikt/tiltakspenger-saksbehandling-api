package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldHaSisteVentestatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
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
            taBehandling(tac, sak.id, behandlingId, beslutter)!!

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
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())!!

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
