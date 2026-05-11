package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.right
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.tilMeldekortApiDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.FeilVedSendingTilMeldekortApi
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
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

    @Test
    fun `iverksatt meldekortbehandling flagger saken for sending og payload inneholder meldekortvedtak`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val result = iverksettSøknadsbehandlingOgMeldekortbehandling(tac)!!
            val sak = result.first
            val meldekortvedtak = result.fourth

            val sakerForSending = tac.sakContext.sakRepo.hentForSendingTilMeldekortApi()
            sakerForSending.map { it.id } shouldBe listOf(sak.id)

            // Bruk en tracking-klient som fanger opp hva som faktisk sendes
            val sendteSaker = mutableListOf<Sak>()
            val trackingKlient = object : MeldekortApiKlient {
                override suspend fun sendSak(sak: Sak): Either<FeilVedSendingTilMeldekortApi, Unit> {
                    sendteSaker.add(sak)
                    return Unit.right()
                }
            }

            SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = trackingKlient,
            ).sendSaker()

            sendteSaker shouldHaveSize 1
            sendteSaker.single().meldekortvedtaksliste.map { it.id } shouldBe listOf(meldekortvedtak.id)

            tac.sakContext.sakRepo.hentForSendingTilMeldekortApi() shouldBe emptyList()
        }
    }

    @Test
    fun `Sak tilMeldekortApiDTO og JSON-serialisering inneholder meldekortvedtak med dager`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val result = iverksettSøknadsbehandlingOgMeldekortbehandling(tac)!!
            val sak = result.first
            val meldekortvedtak = result.fourth

            val dto: SakTilMeldekortApiDTO = sak.tilMeldekortApiDTO()

            // Strukturelle invarianter på DTO-en
            dto.fnr shouldBe sak.fnr.verdi
            dto.sakId shouldBe sak.id.toString()
            dto.saksnummer shouldBe sak.saksnummer.toString()
            dto.meldekortvedtak shouldHaveSize 1

            val vedtakDto = dto.meldekortvedtak.single()
            vedtakDto.id shouldBe meldekortvedtak.id.toString()
            vedtakDto.opprettet shouldBe meldekortvedtak.opprettet
            vedtakDto.erKorrigering shouldBe meldekortvedtak.erKorrigering
            vedtakDto.erAutomatiskBehandlet shouldBe meldekortvedtak.erAutomatiskBehandlet
            vedtakDto.meldeperiodebehandlinger shouldHaveSize meldekortvedtak.meldeperiodebehandlinger.size

            // Behandlinger må mappes parvis med riktig beregning (kjedeId-match)
            vedtakDto.meldeperiodebehandlinger.zip(meldekortvedtak.meldeperiodebehandlinger).forEach { (dto, behandling) ->
                dto.meldeperiodeId shouldBe behandling.meldeperiodeId.toString()
                dto.meldeperiodeKjedeId shouldBe behandling.kjedeId.toString()
                dto.brukersMeldekortId shouldBe behandling.brukersMeldekort?.id?.toString()
                dto.periodeDTO.fraOgMed shouldBe behandling.periode.fraOgMed.toString()
                dto.periodeDTO.tilOgMed shouldBe behandling.periode.tilOgMed.toString()
                dto.dager shouldHaveSize 14
            }

            // Round-trip via JSON: alle felter må kunne (de)serialiseres uten å miste data
            val json = serialize(dto)
            json shouldContain meldekortvedtak.id.toString()
            json shouldContain sak.fnr.verdi

            val deserialisert = deserialize<SakTilMeldekortApiDTO>(json)
            deserialisert shouldBe dto
        }
    }
}
