package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendFørstegangsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendFørstegangsbehandlingTilBeslutningReturnerRespons
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import org.junit.jupiter.api.Test

class SendFørstegangsbehandlingTilBeslutterTest {
    @Test
    fun `send til beslutter endrer status på behandlingen`() = runTest {
        with(TestApplicationContext()) {
            val saksbehandler = saksbehandler()
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.startBehandling(tac, saksbehandler = saksbehandler)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                }
                sendFørstegangsbehandlingTilBeslutningForBehandlingId(
                    tac,
                    sak.id,
                    behandlingId,
                    saksbehandler,
                    innvilgelsesperiode = søknad.vurderingsperiode(),
                    eksternDeltagelseId = søknad.tiltak.id,
                )
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.KLAR_TIL_BESLUTNING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                    it.valgteTiltaksdeltakelser?.periodisering?.perioderMedVerdi?.size shouldBe 1
                    it.valgteTiltaksdeltakelser?.periodisering?.perioderMedVerdi?.firstOrNull()?.verdi?.eksternDeltagelseId shouldBe søknad.tiltak.id
                    it.valgteTiltaksdeltakelser?.periodisering?.totalePeriode shouldBe søknad.vurderingsperiode()
                }
            }
        }
    }

    @Test
    fun `send til beslutter - feiler hvis behandlingsperioden er utenfor deltakelsesperioden`() = runTest {
        with(TestApplicationContext()) {
            val saksbehandler = saksbehandler()
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.startBehandling(tac, saksbehandler = saksbehandler)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                }
                val tiltaksdeltakelseFom = behandling.saksopplysninger.tiltaksdeltagelse.first().deltagelseFraOgMed!!
                val tiltaksdeltakelseTom = behandling.saksopplysninger.tiltaksdeltagelse.first().deltagelseTilOgMed!!

                val response = sendFørstegangsbehandlingTilBeslutningReturnerRespons(
                    tac,
                    sak.id,
                    behandlingId,
                    saksbehandler,
                    innvilgelsesperiode = Periode(tiltaksdeltakelseFom.minusWeeks(1), tiltaksdeltakelseTom),
                    eksternDeltagelseId = søknad.tiltak.id,
                )

                response.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    }

    @Test
    fun `send til beslutter - feiler hvis behandlingen eies av en annen saksbehandler`() = runTest {
        with(TestApplicationContext()) {
            val saksbehandler = saksbehandler()
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.startBehandling(tac, saksbehandler = saksbehandler)
                val behandlingId = behandling.id
                tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                    it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    it.saksbehandler shouldBe saksbehandler.navIdent
                    it.beslutter shouldBe null
                }

                val response = sendFørstegangsbehandlingTilBeslutningReturnerRespons(
                    tac,
                    sak.id,
                    behandlingId,
                    saksbehandler(navIdent = "Z999999"),
                    innvilgelsesperiode = søknad.vurderingsperiode(),
                    eksternDeltagelseId = søknad.tiltak.id,
                )

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe objectMapper.writeValueAsString(Standardfeil.behandlingenEiesAvAnnenSaksbehandler(saksbehandler.navIdent))
            }
        }
    }
}
