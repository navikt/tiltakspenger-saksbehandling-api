package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettAutomatiskBehandlingKlarTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjenn
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennAutomatiskSaksbehandletBehandling
import org.junit.jupiter.api.Test

class UnderkjennRammebehandlingTest {

    /**
     * Kjører mot postgres fordi den er grunnsettet for at en underkjenning lagres og leses tilbake.
     * `Attesteringsstatus.SENDT_TILBAKE` mappes kun her; de øvrige underkjenn-testene kan kjøre in-memory.
     */
    @Test
    fun `sjekk at begrunnelse kan sendes inn`() {
        runTest {
            withTestApplicationContextAndPostgres { tac ->
                val (_, _, behandlingId, _) = this.underkjenn(tac)

                val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
                oppdatertBehandling.attesteringer.single().let {
                    it shouldBe
                        Attestering(
                            // Ignorerer id+tidspunkt
                            id = it.id,
                            tidspunkt = it.tidspunkt,
                            status = Attesteringsstatus.SENDT_TILBAKE,
                            begrunnelse = NonBlankString.create("send_tilbake_begrunnelse"),
                            beslutter = ObjectMother.beslutter().navIdent,
                        )
                }
                oppdatertBehandling.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            }
        }
    }

    @Test
    fun `send tilbake - automatisk saksbehandlet - tildeles beslutteren som underkjenner når hen også har saksbehandlerrollen`() {
        runTest {
            withTestApplicationContext { tac ->
                val underkjenner = ObjectMother.saksbehandlerOgBeslutter()
                val (_, _, behandlingId, _) = this.underkjennAutomatiskSaksbehandletBehandling(tac, beslutter = underkjenner)

                val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
                oppdatertBehandling.attesteringer.single().let {
                    it shouldBe
                        Attestering(
                            // Ignorerer id+tidspunkt
                            id = it.id,
                            tidspunkt = it.tidspunkt,
                            status = Attesteringsstatus.SENDT_TILBAKE,
                            begrunnelse = NonBlankString.create("send_tilbake_begrunnelse"),
                            beslutter = underkjenner.navIdent,
                        )
                }
                oppdatertBehandling.saksbehandler shouldBe underkjenner.navIdent
                oppdatertBehandling.beslutter shouldBe null
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                (oppdatertBehandling as Søknadsbehandling).automatiskSaksbehandlet shouldBe false
                // Resultatet den automatiske saksbehandlingen kom fram til nullstilles, slik at saksbehandler tar stilling på nytt.
                oppdatertBehandling.resultat shouldBe null
            }
        }
    }

    @Test
    fun `send tilbake - automatisk saksbehandlet - beslutter uten saksbehandlerrolle tildeles ikke, og saken blir klar til behandling`() {
        runTest {
            withTestApplicationContext { tac ->
                val underkjenner = ObjectMother.beslutter()
                val (_, _, behandlingId, _) = this.underkjennAutomatiskSaksbehandletBehandling(tac, beslutter = underkjenner)

                val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
                oppdatertBehandling.attesteringer.single().beslutter shouldBe underkjenner.navIdent
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                (oppdatertBehandling as Søknadsbehandling).automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.resultat shouldBe null
            }
        }
    }

    @Test
    fun `send tilbake - automatisk saksbehandlet - saksbehandler uten beslutterrolle kan ta og underkjenne`() {
        runTest {
            withTestApplicationContext { tac ->
                val saksbehandler = ObjectMother.saksbehandler()
                val (_, _, behandlingId, _) = this.underkjennAutomatiskSaksbehandletBehandling(tac, beslutter = saksbehandler)

                val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
                oppdatertBehandling.attesteringer.single().let {
                    it.status shouldBe Attesteringsstatus.SENDT_TILBAKE
                    it.beslutter shouldBe saksbehandler.navIdent
                }
                oppdatertBehandling.saksbehandler shouldBe saksbehandler.navIdent
                oppdatertBehandling.beslutter shouldBe null
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                (oppdatertBehandling as Søknadsbehandling).automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.resultat shouldBe null
            }
        }
    }

    @Test
    fun `automatisk saksbehandlet - saksbehandler uten beslutterrolle kan ikke iverksette`() {
        runTest {
            withTestApplicationContext { tac ->
                val saksbehandler = ObjectMother.saksbehandler()
                val (sak, _, behandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
                taRammebehandlinger(tac, listOf(sak.id to behandling.id), saksbehandler = saksbehandler)

                iverksettForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    beslutter = saksbehandler,
                    forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
                ) shouldBe null

                tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).let {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                    it.beslutter shouldBe saksbehandler.navIdent
                }
            }
        }
    }

    @Test
    fun `manuelt saksbehandlet - saksbehandler uten beslutterrolle kan ikke ta behandlingen til beslutning`() {
        runTest {
            withTestApplicationContext { tac ->
                val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

                taRammebehandlinger(
                    tac = tac,
                    behandlinger = listOf(sak.id to behandlingId),
                    saksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999"),
                    forventet = ForventetRespons(status = 403, contentType = "application/json; charset=UTF-8"),
                ) shouldBe null

                tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).let {
                    it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                    it.beslutter shouldBe null
                }
            }
        }
    }

    @Test
    fun `send tilbake - manuelt saksbehandlet - resultatet beholdes`() {
        runTest {
            withTestApplicationContext { tac ->
                val (_, _, behandlingId, _) = this.underkjenn(tac)

                val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                (oppdatertBehandling as Søknadsbehandling).automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.resultat.shouldBeInstanceOf<Søknadsbehandlingsresultat.Innvilgelse>()
            }
        }
    }
}
