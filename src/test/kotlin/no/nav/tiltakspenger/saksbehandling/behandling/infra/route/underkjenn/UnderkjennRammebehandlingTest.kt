package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjenn
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennAutomatiskSaksbehandletBehandling
import org.junit.jupiter.api.Test

class UnderkjennRammebehandlingTest {

    @Test
    fun `sjekk at begrunnelse kan sendes inn`() {
        runTest {
            withTestApplicationContext { tac ->
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
    fun `send tilbake - automatisk saksbehandlet - blir klar til behandling og ikke tildelt`() {
        runTest {
            withTestApplicationContext { tac ->
                val (_, _, behandlingId, _) = this.underkjennAutomatiskSaksbehandletBehandling(tac)

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
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                (oppdatertBehandling as Søknadsbehandling).automatiskSaksbehandlet shouldBe false
            }
        }
    }
}
