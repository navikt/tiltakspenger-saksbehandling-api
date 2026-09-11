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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TaOgOvertaRammebehandlingerTest {

    @Test
    fun `må ha både saksbehandler- og beslutterrolle for å ta flere behandlinger med ulike rollekrav`() {
        withTestApplicationContext { tac ->
            val (sak1, _, behandling1) = opprettSøknadsbehandlingKlarTilBehandling(tac)
            val (sak2, _, behandling2) = sendSøknadsbehandlingTilBeslutning(tac) // Denne setter en standard saksbehandler - spør Anders :)

            val behandlinger = listOf(sak1.id to behandling1.id, sak2.id to behandling2)

            // Ingen saksbeholder- eller beslutterrolle
            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
                saksbehandler = ObjectMother.saksbehandlerUtenTilgang(),
                forventet = ForventetRespons(status = 403, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling1.id).saksbehandler shouldBe null
            tac.behandlingContext.rammebehandlingRepo.hent(behandling2).beslutter shouldBe null

            // Bare saksbehandlerrolle
            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "A12345"),
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

            tac.behandlingContext.rammebehandlingRepo.hent(behandling1.id).saksbehandler shouldBe null
            tac.behandlingContext.rammebehandlingRepo.hent(behandling2).beslutter shouldBe null

            // Bare beslutterrolle
            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
                saksbehandler = ObjectMother.beslutter(navIdent = "B12345"),
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

            tac.behandlingContext.rammebehandlingRepo.hent(behandling1.id).saksbehandler shouldBe null
            tac.behandlingContext.rammebehandlingRepo.hent(behandling2).beslutter shouldBe null

            // Både saksbehehandler- og beslutterrolle
            taRammebehandlinger(tac, behandlinger = behandlinger, saksbehandler = ObjectMother.saksbehandlerOgBeslutter(navIdent = "O12345"))

            tac.behandlingContext.rammebehandlingRepo.hent(behandling1.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "O12345"
            }

            tac.behandlingContext.rammebehandlingRepo.hent(behandling2).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                it.beslutter shouldBe "O12345"
            }
        }
    }

    @Test
    fun `rammebehandling og saksnummer sendes til saksbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            val behandlinger = listOf(sak.id to behandling.id)

            taRammebehandlinger(tac, behandlinger = behandlinger)!!.also { (_, responsJson) ->
                responsJson!!.get("behandlinger").also { liste ->
                    liste.size() shouldBe 1
                    liste[0].get("behandlingId").asString() shouldBe behandling.id.toString()
                    liste[0].get("saksnummer").asString() shouldBe sak.saksnummer.toString()
                }
            }
        }
    }

    @Test
    fun `saker returneres ikke som standard`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            taRammebehandlinger(tac, behandlinger = listOf(sak.id to behandling.id))!!.also { (_, responsJson) ->
                responsJson!!.get("saker").size() shouldBe 0
            }
        }
    }

    @Test
    fun `saker returneres ikke når returnerSaker er false`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            taRammebehandlinger(
                tac,
                behandlinger = listOf(sak.id to behandling.id),
                returnerSaker = false,
            )!!.also { (_, responsJson) ->
                responsJson!!.get("saker").size() shouldBe 0
            }
        }
    }

    @Test
    fun `saker returneres når returnerSaker er true`() {
        withTestApplicationContext { tac ->
            val (sak1, _, behandling1) = opprettSøknadsbehandlingKlarTilBehandling(tac)
            val (sak2, _, behandling2) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            val behandlinger = listOf(sak1.id to behandling1.id, sak2.id to behandling2.id)

            taRammebehandlinger(tac, behandlinger = behandlinger, returnerSaker = true)!!.also { (_, responsJson) ->
                responsJson!!.get("saker").also { saker ->
                    saker.size() shouldBe 2
                    saker[0].get("sakId").asString() shouldBe sak1.id.toString()
                    saker[0].get("saksnummer").asString() shouldBe sak1.saksnummer.toString()
                    saker[0].get("fnr").asString() shouldBe sak1.fnr.verdi
                    saker[1].get("sakId").asString() shouldBe sak2.id.toString()
                    saker[1].get("saksnummer").asString() shouldBe sak2.saksnummer.toString()
                    saker[1].get("fnr").asString() shouldBe sak2.fnr.verdi
                }
            }
        }
    }

    @Test
    fun `ingen behandlinger tildeles når en i listen ikke kan tas`() {
        withTestApplicationContext { tac ->
            val (sak1, _, behandling1) = opprettSøknadsbehandlingKlarTilBehandling(tac)
            val (sak2, _, behandling2) = opprettSøknadsbehandlingUnderBehandling(tac)

            val behandlinger = listOf(sak1.id to behandling1.id, sak2.id to behandling2.id)

            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "A12345"),
                forventet = ForventetRespons.json(
                    status = 400,
                    json = """
                {
                  "melding": "Behandlingen har allerede en saksbehandler.",
                  "kode": "behandlingen_har_allerede_en_saksbehandler"
                }
                    """.trimIndent(),
                    contentType = "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling1.id).saksbehandler shouldBe null
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling2.id).saksbehandler shouldBe "Z12345"
        }
    }

    @Test
    fun `saksbehandler må alltid tildele seg minst en sak`() {
        withTestApplicationContext { tac ->

            taRammebehandlinger(
                tac,
                behandlinger = emptyList(),
                forventet = ForventetRespons.json(
                    status = 400,
                    json = """
                {
                  "melding": "Du må sende inn minst en behandling.",
                  "kode": "må_ha_minst_en_behandling"
                }
                    """.trimIndent(),
                    contentType = "application/json; charset=UTF-8",
                ),
            ) shouldBe null
        }
    }

    /**
     * Kjører mot postgres fordi den er grunnsettet for `taBehandlingBeslutter` og `overtaBeslutter` i [no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingPostgresRepo].
     * Saksbehandlervarianten over kjører også mot postgres for å dekke begge tillatte kildestatuser.
     */
    @Test
    fun `beslutter kan ta og overta behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = sendSøknadsbehandlingTilBeslutning(tac)

            val behandlinger = listOf(sak.id to behandling)

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            }

            tac.clock.spol1timeFrem()

            taRammebehandlinger(tac, behandlinger = behandlinger, saksbehandler = ObjectMother.beslutter()).also {
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe "B12345"
                }
            }

            tac.clock.spol1timeFrem()

            behandlinger.map { (sakId, behandlingId) ->
                overtaBehandling(tac, sakId = sakId, behandlingId = behandlingId, overtarFra = "B12345", saksbehandler = ObjectMother.beslutter(navIdent = "B123"))!!.also { (_, _, sakJson) ->
                    sakJson.rammebehandlingJson(behandlingId).get("beslutter").asString() shouldBe "B123"
                    tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B123"
                    }
                }
                tac.clock.spol1timeFrem()

                overtaBehandling(tac, sakId = sak.id, behandlingId, overtarFra = "B123", saksbehandler = ObjectMother.beslutter())!!.also { (_, _, sakJson) ->
                    sakJson.rammebehandlingJson(behandlingId).get("beslutter").asString() shouldBe "B12345"
                    tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                        it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                        it.beslutter shouldBe "B12345"
                    }
                }
            }
        }
    }

    @Test
    fun `kan ikke ta behandling som allerede har saksbehandler`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val behandlinger = listOf(sak.id to behandling.id)

            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
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

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke ta en behandling som er klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = sendSøknadsbehandlingTilBeslutning(tac)

            val behandlinger = listOf(sak.id to behandling)

            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
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

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling).beslutter shouldBe null
        }
    }

    @Test
    fun `en bruker uten saksbehandlerrolle kan ikke ta en behandling som er klar til behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            val behandlinger = listOf(sak.id to behandling.id)

            taRammebehandlinger(
                tac,
                behandlinger = behandlinger,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons.json(
                    status = 403,
                    json = """
                    {
                      "melding": "Du må være saksbehandler for å ta denne behandlingen.",
                      "kode": "maa_vaere_saksbehandler"
                    }    
                    """.trimIndent(),
                    contentType = "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId = behandling.id).saksbehandler shouldBe null
        }
    }

    @Test
    fun `en saksbehandler kan ta en behandling som ikke er tildelt`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            val behandlinger = listOf(sak.id to behandling.id)

            taRammebehandlinger(tac, behandlinger = behandlinger)

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `en saksbehandler kan ta flere behandlinger som ikke er tildelt`() {
        withTestApplicationContext { tac ->

            val (sak1, _, behandling1) = opprettSøknadsbehandlingKlarTilBehandling(tac)
            val (sak2, _, behandling2) = opprettSøknadsbehandlingKlarTilBehandling(tac)

            val behandlinger = listOf(sak1.id to behandling1.id, sak2.id to behandling2.id)

            taRammebehandlinger(tac, behandlinger = behandlinger)

            tac.behandlingContext.rammebehandlingRepo.hent(behandling1.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }

            tac.behandlingContext.rammebehandlingRepo.hent(behandling2.id).also {
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
            overtaBehandling(tac, sak.id, behandlingId, "Z12345", ObjectMother.saksbehandler123())!!.also { (_, _, sakJson) ->
                sakJson.rammebehandlingJson(behandlingId).get("saksbehandler").asString() shouldBe "123"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "123"
                }
            }
            tac.clock.spol1timeFrem()
            overtaBehandling(tac, sak.id, behandlingId, "123")!!.also { (_, _, sakJson) ->
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

            overtaBehandling(
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

            overtaBehandling(
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

            overtaBehandling(
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

            taRammebehandlinger(tac = tac, behandlinger = listOf(sak.id to behandlingId), saksbehandler = ObjectMother.beslutter())
            tac.clock.spol1timeFrem()

            overtaBehandling(
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
}
