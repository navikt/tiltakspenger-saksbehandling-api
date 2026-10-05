package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.angre

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import org.junit.jupiter.api.Test

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
class AngreRammebehandlingRouteTest {

    /**
     * Kjører mot postgres fordi den er grunnsettet for `angreBehandling` i [no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingPostgresRepo].
     * De øvrige angre-testene kan kjøre in-memory.
     */
    @Test
    fun `saksbehandler kan angre sendingen til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
            }

            angreRammebehandling(tac, sak.id, behandlingId)!!.also { (_, angretBehandling, sakJson) ->
                angretBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                angretBehandling.saksbehandler shouldBe "Z12345"
                angretBehandling.sendtTilBeslutning shouldBe null
                sakJson.rammebehandlingJson(behandlingId).get("status").asString() shouldBe "UNDER_BEHANDLING"
                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe "Z12345"
                    it.sendtTilBeslutning shouldBe null
                }
            }

            tac.sessionFactory.hentSaksstatistikk(sak.id).last().also {
                it.hendelse shouldBe "saksbehandler_angrer"
                it.behandlingStatus shouldBe StatistikkBehandlingStatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    /**
     * Kjører mot postgres slik at `UNDER_BESLUTNING`-grenen i vakten til `angreBehandling` øves mot ekte SQL.
     */
    @Test
    fun `saksbehandler kan angre en behandling som allerede er tatt av en beslutter`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taRammebehandlinger(
                tac = tac,
                behandlinger = listOf(sak.id to behandlingId),
                saksbehandler = ObjectMother.beslutter(),
            )!!
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
                it.beslutter shouldBe "B12345"
            }

            angreRammebehandling(tac, sak.id, behandlingId)!!.also { (_, angretBehandling, sakJson) ->
                angretBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                angretBehandling.beslutter shouldBe null
                sakJson.rammebehandlingJson(behandlingId).get("status").asString() shouldBe "UNDER_BEHANDLING"
            }

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
                it.beslutter shouldBe null
                it.sendtTilBeslutning shouldBe null
            }

            tac.sessionFactory.hentSaksstatistikk(sak.id).last().also {
                it.hendelse shouldBe "saksbehandler_angrer"
                it.behandlingStatus shouldBe StatistikkBehandlingStatus.UNDER_BEHANDLING
            }
        }
    }

    /**
     * Rammebehandlingen har ingen egen feil for en behandling som ikke finnes på saken.
     * `hentSakOgRammebehandling` kaster, og exception-handleren svarer 500.
     */
    @Test
    fun `saksbehandler kan ikke angre en behandling fra en annen sak`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = sendSøknadsbehandlingTilBeslutning(tac)
            val (_, _, behandlingIdPåAnnenSak) = sendSøknadsbehandlingTilBeslutning(tac)

            angreRammebehandling(
                tac,
                sak.id,
                behandlingIdPåAnnenSak,
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

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingIdPåAnnenSak).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `beslutter kan ikke angre sendingen til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            angreRammebehandling(
                tac,
                sak.id,
                behandlingId,
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

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }
        }
    }

    @Test
    fun `saksbehandler kan angre en revurdering sendt til beslutning`() {
        withTestApplicationContext { tac ->
            // Innvilgelsen starter mandag 2. januar 2023, slik at dagen som beholder rett etter stans (mandag) er en hverdag.
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
            tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
            }

            angreRammebehandling(tac, sak.id, revurdering.id)!!.also { (_, angretRevurdering, sakJson) ->
                angretRevurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                angretRevurdering.saksbehandler shouldBe "Z12345"
                sakJson.rammebehandlingJson(revurdering.id).get("status").asString() shouldBe "UNDER_BEHANDLING"
            }
        }
    }

    @Test
    fun `annen saksbehandler kan ikke angre sendingen til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            angreRammebehandling(
                tac,
                sak.id,
                behandlingId,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "Z999999"),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandleren som er tildelt behandlingen for å angre.",
                      "kode": "maa_vaere_saksbehandler_for_behandlingen"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `kan ikke angre en behandling som ikke er sendt til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            angreRammebehandling(
                tac,
                sak.id,
                behandling.id,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Kan ikke angre behandling med status UNDER_BEHANDLING.",
                      "kode": "behandlingen_kan_ikke_angres"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }
}
