package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningReturnerRespons
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
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
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
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
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
                it.valgteTiltaksdeltakelser?.perioderMedVerdi?.size shouldBe 1
                søknad.shouldBeInstanceOf<InnvilgbarSøknad>()
                it.valgteTiltaksdeltakelser?.perioderMedVerdi?.firstOrNull()?.verdi?.eksternDeltakelseId shouldBe søknad.tiltak.id
                it.valgteTiltaksdeltakelser?.totalPeriode shouldBe søknad.tiltaksdeltakelseperiodeDetErSøktOm()
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
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
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

    @Test
    fun `kan ikke sende til beslutter hvis resultat er oppdatert til ikke valgt`() {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac,
                saksbehandler = saksbehandler,
            )

            val behandlingId = behandling.id

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.IkkeValgtResultat(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                ),
            )

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.resultat.shouldBeNull()
            }

            sendSøknadsbehandlingTilBeslutningReturnerRespons(
                tac,
                sak.id,
                behandlingId,
                saksbehandler,
            ).also {
                it.status shouldBe HttpStatusCode.InternalServerError
            }

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            }
        }
    }
}
