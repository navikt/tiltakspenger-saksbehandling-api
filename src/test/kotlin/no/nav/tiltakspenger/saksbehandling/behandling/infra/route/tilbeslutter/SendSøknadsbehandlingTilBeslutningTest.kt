package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningReturnerRespons
import org.junit.jupiter.api.Test

class SendSøknadsbehandlingTilBeslutningTest {
    @Test
    fun `send til beslutter endrer status på behandlingen`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac,
                saksbehandler = saksbehandler,
            )
            val behandlingId = behandling.id

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }

            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                behandlingId,
                saksbehandler,
            )

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.shouldBeInstanceOf<Søknadsbehandling>()
                it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
                it.valgteTiltaksdeltakelser?.periodisering?.perioderMedVerdi?.size shouldBe 1
                it.valgteTiltaksdeltakelser?.periodisering?.perioderMedVerdi?.firstOrNull()?.verdi?.eksternDeltagelseId shouldBe søknad.tiltak.id
                it.valgteTiltaksdeltakelser?.periodisering?.totalPeriode shouldBe søknad.tiltaksdeltagelseperiodeDetErSøktOm()
            }
        }
    }

    @Test
    fun `send til beslutter - feiler hvis behandlingen eies av en annen saksbehandler`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val (sak, _, behandling) = this.opprettSøknadsbehandlingUnderBehandling(
                tac,
                saksbehandler = saksbehandler,
            )
            val behandlingId = behandling.id
            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }

            val response = sendSøknadsbehandlingTilBeslutningReturnerRespons(
                tac,
                sak.id,
                behandlingId,
                saksbehandler(navIdent = "Z999999"),
            )

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldBe objectMapper.writeValueAsString(
                Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
                    saksbehandler.navIdent,
                ),
            )
        }
    }
}
