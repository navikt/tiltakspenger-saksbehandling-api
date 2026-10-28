package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.hentTilbake

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettAutomatiskBehandlingKlarTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import org.json.JSONObject
import org.junit.jupiter.api.Test

class HentTilbakeRammebehandlingRouteTest {

    @Test
    fun `saksbehandler kan hente tilbake en automatisk behandlet søknadsbehandling klar til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.shouldBeInstanceOf<Søknadsbehandling>().automatiskSaksbehandlet shouldBe true
                it.resultat shouldNotBe null
            }

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                saksbehandler = saksbehandler,
            )!!.also { (oppdatertSak, hentetBehandling, sakJson) ->
                hentetBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                hentetBehandling.saksbehandler shouldBe "Z12345"
                hentetBehandling.sendtTilBeslutning shouldBe null
                hentetBehandling.shouldBeInstanceOf<Søknadsbehandling>().automatiskSaksbehandlet shouldBe false
                hentetBehandling.resultat shouldBe null

                oppdatertSak.hentRammebehandling(opprettetBehandling.id).shouldNotBeNull().also { hentetBehandlingRepo ->
                    hentetBehandlingRepo.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    hentetBehandlingRepo.saksbehandler shouldBe "Z12345"
                    hentetBehandlingRepo.sendtTilBeslutning shouldBe null
                    hentetBehandlingRepo.shouldBeInstanceOf<Søknadsbehandling>().automatiskSaksbehandlet shouldBe false
                    hentetBehandlingRepo.resultat shouldBe null
                }

                sakJson.rammebehandlingJson(opprettetBehandling.id).also { behandlingJson ->
                    behandlingJson.get("status").asString() shouldBe "UNDER_BEHANDLING"
                    behandlingJson.get("saksbehandler").asString() shouldBe "Z12345"
                    behandlingJson.get("beslutter").isNull shouldBe true
                    behandlingJson.get("automatiskSaksbehandlet").asBoolean() shouldBe false
                }
            }

            tac.sessionFactory.hentSaksstatistikk(sak.id).last().also {
                it.hendelse shouldBe "oppdatert_saksbehandler_beslutter"
                it.behandlingStatus shouldBe StatistikkBehandlingStatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
                it.ansvarligBeslutter shouldBe null
            }
        }
    }

    @Test
    fun `saksbehandler som har hentet tilbake kan behandle, sende til beslutning og få behandlingen iverksatt`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val innvilgelsesperioder = innvilgelsesperioder()
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(
                tac = tac,
                tiltaksdeltakelse = innvilgelsesperioder.valgteTiltaksdeltagelser.verdier.distinct().single(),
            )
            hentTilbakeRammebehandling(tac, sak.id, opprettetBehandling.id, saksbehandler)!!

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                innvilgelsesperioder = innvilgelsesperioder,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, opprettetBehandling.id, saksbehandler = saksbehandler)
            taRammebehandlinger(tac, listOf(sak.id to opprettetBehandling.id), beslutter)!!
            val (_, _, vedtattBehandling, _) = iverksettForBehandlingId(tac, sak.id, opprettetBehandling.id, beslutter = beslutter)!!

            vedtattBehandling.status shouldBe Rammebehandlingsstatus.VEDTATT
            vedtattBehandling.saksbehandler shouldBe saksbehandler.navIdent
            vedtattBehandling.beslutter shouldBe beslutter.navIdent
            vedtattBehandling.shouldBeInstanceOf<Søknadsbehandling>().automatiskSaksbehandlet shouldBe false
        }
    }

    @Test
    fun `saksbehandler kan ikke hente tilbake samme behandling to ganger`() {
        withTestApplicationContext { tac ->
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            val (_, førsteHentTilbake, _) = hentTilbakeRammebehandling(tac, sak.id, opprettetBehandling.id)!!

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999999"),
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kan ikke hente tilbake behandling med status UNDER_BEHANDLING.",
                      "kode": "behandlingen_må_være_klar_til_beslutning"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id) shouldBe førsteHentTilbake
        }
    }

    @Test
    fun `beslutter uten saksbehandlerrolle kan ikke hente tilbake`() {
        withTestApplicationContext { tac ->
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            val behandlingFør = tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id)

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Saksbehandler B12345 mangler rollen SAKSBEHANDLER. Saksbehandlers roller: Saksbehandlerroller(value=[BESLUTTER])",
                      "kode": "tilgang_nektet_krev_rolle"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id) shouldBe behandlingFør
        }
    }

    @Test
    fun `bruker med både saksbehandler- og beslutterrolle kan hente tilbake`() {
        withTestApplicationContext { tac ->
            val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter()
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)

            val (_, hentetBehandling, sakJson) = hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                saksbehandler = saksbehandlerOgBeslutter,
            )!!

            hentetBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            hentetBehandling.saksbehandler shouldBe saksbehandlerOgBeslutter.navIdent
            hentetBehandling.beslutter shouldBe null
            sakJson.rammebehandlingJson(opprettetBehandling.id).get("saksbehandler").asString() shouldBe
                saksbehandlerOgBeslutter.navIdent
        }
    }

    @Test
    fun `kan ikke hente tilbake en manuelt behandlet søknadsbehandling klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            val behandlingFør = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999999"),
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen er ikke automatisk behandlet og kan ikke hentes tilbake.",
                      "kode": "behandlingen_må_være_automatisk_behandlet"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId) shouldBe behandlingFør
        }
    }

    @Test
    fun `kan ikke hente tilbake en revurdering klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(2 til 31.januar(2023)),
            )
            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(HjemmelForStans.Alder),
                stansFraOgMed = sak.førsteDagSomGirRett!!.plusDays(1),
                harValgtStansFraFørsteDagSomGirRett = false,
            )
            sendRevurderingTilBeslutningForBehandlingId(tac, sak.id, revurdering.id)
            val revurderingFør = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kun søknadsbehandlinger kan hentes tilbake.",
                      "kode": "behandlingen_må_være_søknadsbehandling"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id) shouldBe revurderingFør
        }
    }

    @Test
    fun `kan ikke hente tilbake en søknadsbehandling som er under behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingFør = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kan ikke hente tilbake behandling med status UNDER_BEHANDLING.",
                      "kode": "behandlingen_må_være_klar_til_beslutning"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id) shouldBe behandlingFør
        }
    }

    @Test
    fun `kan ikke hente tilbake en automatisk behandling som en beslutter har tatt`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to opprettetBehandling.id), ObjectMother.beslutter())!!
            val behandlingUnderBeslutning = tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id)

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kan ikke hente tilbake behandling med status UNDER_BESLUTNING.",
                      "kode": "behandlingen_må_være_klar_til_beslutning"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id) shouldBe behandlingUnderBeslutning
        }
    }

    @Test
    fun `kan ikke hente tilbake en automatisk behandling som beslutter har satt på vent`() {
        withTestApplicationContextAndPostgres { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            taRammebehandlinger(tac, listOf(sak.id to opprettetBehandling.id), beslutter)!!
            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = opprettetBehandling.id,
                saksbehandler = beslutter,
            )!!
            val behandlingPåVent = tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id)
            behandlingPåVent.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            behandlingPåVent.ventestatus.erSattPåVent shouldBe true

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetBehandling.id,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen er satt på vent.",
                      "kode": "behandlingen_kan_ikke_være_satt_på_vent"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id) shouldBe behandlingPåVent
        }
    }

    @Test
    fun `kan ikke hente tilbake en behandling fra en annen sak`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            val (_, _, behandlingPåAnnenSak) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            val behandlingFør = tac.behandlingContext.rammebehandlingRepo.hent(behandlingPåAnnenSak.id)

            hentTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingPåAnnenSak.id,
                forventet = ForventetRespons.json(
                    500,
                    """
                    {
                      "melding": "Noe gikk galt på serversiden",
                      "kode": "server_feil"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingPåAnnenSak.id) shouldBe behandlingFør
        }
    }

    @Test
    fun `sakDTO annonserer HentTilbake for saksbehandler før, men ikke etter at behandlingen er hentet tilbake`() {
        withTestApplicationContext { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (sak, _, opprettetBehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
            val sakJsonFør = hentSakForSaksnummer(tac, sak.saksnummer, saksbehandler)!!

            sakJsonFør.gyldigeKommandoer(opprettetBehandling.id) shouldBe listOf("HentTilbake")

            val (_, _, sakJsonEtter) = hentTilbakeRammebehandling(tac, sak.id, opprettetBehandling.id, saksbehandler)!!

            val kommandoerEtter = sakJsonEtter.rammebehandlingJson(opprettetBehandling.id).get("gyldigeKommandoer")
            (0 until kommandoerEtter.size()).map { kommandoerEtter.get(it).asString() } shouldBe
                listOf("Saksbehandle", "LeggTilbakeSaksbehandler", "SettPåVent", "Avbryt")
        }
    }
}

private fun JSONObject.gyldigeKommandoer(behandlingId: RammebehandlingId): List<String> {
    val rammebehandlinger = getJSONArray("rammebehandlinger")
    val behandlingJson = (0 until rammebehandlinger.length())
        .map { rammebehandlinger.getJSONObject(it) }
        .single { it.getString("id") == behandlingId.toString() }
    val kommandoer = behandlingJson.getJSONArray("gyldigeKommandoer")
    return (0 until kommandoer.length()).map { kommandoer.getString(it) }
}
