package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TaOgOvertaRammebehandlingTest {

    @Test
    fun `en saksbehandler kan ta en behandling som ikke er tildelt`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            taBehandling(tac, sak.id, behandling.id)!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `en bruker uten saksbehandlerrolle kan ikke ta en behandling som er klar til behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            taBehandling(
                tac,
                sak.id,
                behandling.id,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandler for å ta denne behandlingen.",
                      "kode": "maa_vaere_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).saksbehandler shouldBe null
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke ta en behandling som er klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            taBehandling(
                tac,
                sak.id,
                behandlingId,
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å ta denne behandlingen.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutter shouldBe null
        }
    }

    @Test
    fun `kan ikke ta behandling som allerede har saksbehandler`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            taBehandling(
                tac,
                sak.id,
                behandling.id,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999999"),
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen har allerede en saksbehandler.",
                      "kode": "behandlingen_har_allerede_en_saksbehandler"
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
    fun `saksbehandler kan overta behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingId = behandling.id
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "Z12345", ObjectMother.saksbehandler123())!!.also { (_, _, sakJson) ->
                sakJson.rammebehandlingJson(behandlingId).get("saksbehandler").asString() shouldBe "123"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "123"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "123")!!.also { (_, _, sakJson) ->
                sakJson.rammebehandlingJson(behandlingId).get("saksbehandler").asString() shouldBe "Z12345"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                }
            }
        }
    }

    @Test
    fun `saksbehandler kan overta automatisk behandling som er satt på vent`() {
        withTestApplicationContextAndPostgres { tac ->
            val iDag = LocalDate.now(tac.clock)
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
                    fom = iDag.plusDays(3),
                    tom = iDag.plusMonths(3),
                    status = TiltakDeltakerstatus.VenterPåOppstart,
                ),
            )
            tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(
                behandling,
                CorrelationId.generate(),
            )
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                it.ventestatus.erSattPåVent shouldBe true
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
            }
            tac.clock.spol1timeFrem()

            overtaBehanding(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                overtarFra = AUTOMATISK_SAKSBEHANDLER_ID,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `kan ikke overta en behandling det er mindre enn ett minutt siden noe ble gjort på`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            overtaBehanding(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                overtarFra = "Z12345",
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen er under aktiv behandling og kan ikke overtas. Prøv igjen innen 1 time",
                      "kode": "behandlingen_er_under_aktiv_behandling"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).saksbehandler shouldBe "Z12345"
        }
    }

    @Test
    fun `en bruker uten saksbehandlerrolle kan ikke overta en behandling som er under behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            tac.clock.spol1timeFrem()

            overtaBehanding(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                overtarFra = "Z12345",
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandler for å overta denne behandlingen",
                      "kode": "maa_vaere_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).saksbehandler shouldBe "Z12345"
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke overta en behandling som er under beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            tac.clock.spol1timeFrem()
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter())!!
            tac.clock.spol1timeFrem()

            overtaBehanding(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                overtarFra = "B12345",
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å overta denne behandlingen",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutter shouldBe "B12345"
        }
    }

    /**
     * Kjører mot postgres fordi den er grunnsettet for `taBehandlingBeslutter` og `overtaBeslutter` i [no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingPostgresRepo].
     * Saksbehandlervarianten over kjører også mot postgres for å dekke begge tillatte kildestatuser.
     */
    @Test
    fun `beslutter kan ta og overta behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            }
            tac.clock.spol1timeFrem()
            taBehandling(tac, sak.id, behandlingId, ObjectMother.beslutter()).also {
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "B12345", ObjectMother.beslutter("B123"))!!.also { (_, _, sakJson) ->
                sakJson.rammebehandlingJson(behandlingId).get("beslutter").asString() shouldBe "B123"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B123"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehanding(tac, sak.id, behandlingId, "B123", ObjectMother.beslutter())!!.also { (_, _, sakJson) ->
                sakJson.rammebehandlingJson(behandlingId).get("beslutter").asString() shouldBe "B12345"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }
        }
    }
}
