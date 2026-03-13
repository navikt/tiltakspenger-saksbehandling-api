package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.right
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class SendTilMeldekortApiServiceTest {

    @Test
    fun `første søknadsbehandling sender oppdatering til meldekort-api`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac = tac)

            val sakerForSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerForSending.map { it.id } shouldBe listOf(sak.id)

            SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = tac.meldekortContext.meldekortApiHttpClient,
            ).sendSaker()

            val sakerEtterSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerEtterSending shouldBe emptyList()
        }
    }

    @Test
    fun `iverksatt søknadsbehandling sender oppdatering til meldekort-api`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _, _) = iverksettSøknadsbehandling(tac)

            val sakerForSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerForSending.map { it.id } shouldBe listOf(sak.id)

            SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = tac.meldekortContext.meldekortApiHttpClient,
            ).sendSaker()

            val sakerEtterSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerEtterSending shouldBe emptyList()
        }
    }

    @Test
    fun `nytt vedtak iverksatt mens kall til meldekort-api pågår - skal_sendes_til_meldekort_api settes ikke til false`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, rammevedtakSæknad, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac = tac,
            )

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
            )
            sendRevurderingTilBeslutningForBehandlingId(tac, sak.id, revurdering.id)
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = beslutter())

            val sakerForSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerForSending.map { it.id } shouldBe listOf(sak.id)
            sakerForSending.first().rammevedtaksliste.map { it.rammebehandling.id } shouldBe listOf(
                rammevedtakSæknad.rammebehandling.id,
            )

            // Lag en MeldekortApiKlient som venter til vi har iverksatt revurderingen
            val revurderingIverksatt = AtomicBoolean(false)
            val slowMeldekortApiKlient = object : MeldekortApiKlient {
                override suspend fun sendSak(sak: Sak): Either<FeilVedSendingTilMeldekortApi, Unit> {
                    // Vent til revurderingen er iverksatt
                    while (!revurderingIverksatt.get()) {
                        delay(10)
                    }
                    return Unit.right()
                }
            }

            val sendTilMeldekortApiService = SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = slowMeldekortApiKlient,
            )

            // Start sendSaker i bakgrunnen med coroutineScope
            coroutineScope {
                val sendJob = launch {
                    sendTilMeldekortApiService.sendSaker()
                }

                // Iverksett revurderingen mens sendSaker venter
                iverksettForBehandlingId(tac, sak.id, revurdering.id)

                // Signal at revurderingen er iverksatt, så sendSaker kan fortsette
                revurderingIverksatt.set(true)

                // Vent på at sendSaker fullføres
                sendJob.join()
            }

            // Verifiser at saken fortsatt er markert for sending (fordi det kom et nytt vedtak)
            val sakerEtterSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerEtterSending.map { it.id } shouldBe listOf(sak.id)
            sakerEtterSending.first().rammevedtaksliste.map { it.rammebehandling.id } shouldBe listOf(
                rammevedtakSæknad.rammebehandling.id,
                revurdering.id,
            )

            sendTilMeldekortApiService.sendSaker()

            // Skal være sendt etter neste kjøring av jobben
            val sakerEtterNesteSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerEtterNesteSending shouldBe emptyList()
        }
    }
}
