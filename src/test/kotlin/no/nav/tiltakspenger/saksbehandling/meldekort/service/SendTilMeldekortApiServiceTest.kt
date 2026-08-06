package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.right
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.tilMeldekortApiDTO
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
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac = tac)

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe true

            SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = tac.meldekortContext.meldekortApiHttpClient,
            ).sendSak(sak.id)

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe false
        }
    }

    @Test
    fun `iverksatt søknadsbehandling sender oppdatering til meldekort-api`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, _) = iverksettSøknadsbehandling(tac)

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe true

            SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = tac.meldekortContext.meldekortApiHttpClient,
            ).sendSak(sak.id)

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe false
        }
    }

    @Test
    fun `nytt vedtak iverksatt mens kall til meldekort-api pågår - skal_sendes_til_meldekort_api settes ikke til false`() {
        withTestApplicationContextAndPostgres { tac ->
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

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe true
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammevedtaksliste.map { it.rammebehandling.id } shouldBe listOf(
                rammevedtakSæknad.rammebehandling.id,
            )

            // Lag en MeldekortApiKlient som venter til vi har iverksatt revurderingen
            val revurderingIverksatt = AtomicBoolean(false)
            val slowMeldekortApiKlient = object : MeldekortApiKlient {
                override suspend fun sendSak(sak: Sak): Either<HttpKlientError, Unit> {
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

            // Start sending av saken i bakgrunnen med coroutineScope
            coroutineScope {
                val sendJob = launch {
                    sendTilMeldekortApiService.sendSak(sak.id)
                }

                // Iverksett revurderingen mens sendSak venter
                iverksettForBehandlingId(tac, sak.id, revurdering.id)

                // Signal at revurderingen er iverksatt, så sendSak kan fortsette
                revurderingIverksatt.set(true)

                // Vent på at sendSak fullføres
                sendJob.join()
            }

            // Verifiser at saken fortsatt er markert for sending (fordi det kom et nytt vedtak)
            tac.skalSendesTilMeldekortApi(sak.id) shouldBe true
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammevedtaksliste.map { it.rammebehandling.id } shouldBe listOf(
                rammevedtakSæknad.rammebehandling.id,
                revurdering.id,
            )

            sendTilMeldekortApiService.sendSak(sak.id)

            // Skal være sendt etter neste kjøring av jobben
            tac.skalSendesTilMeldekortApi(sak.id) shouldBe false
        }
    }

    @Test
    fun `iverksatt meldekortbehandling flagger saken for sending og payload inneholder meldekortvedtak`() {
        withTestApplicationContextAndPostgres { tac ->
            val result = iverksettSøknadsbehandlingOgMeldekortbehandling(tac)!!
            val sak = result.first
            val meldekortvedtak = result.fourth

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe true

            // Bruk en tracking-klient som fanger opp hva som faktisk sendes
            val sendteSaker = mutableListOf<Sak>()
            val trackingKlient = object : MeldekortApiKlient {
                override suspend fun sendSak(sak: Sak): Either<HttpKlientError, Unit> {
                    sendteSaker.add(sak)
                    return Unit.right()
                }
            }

            SendTilMeldekortApiService(
                sakRepo = tac.sakContext.sakRepo,
                meldekortApiHttpClient = trackingKlient,
            ).sendSak(sak.id)

            sendteSaker shouldHaveSize 1
            sendteSaker.single().meldekortvedtaksliste.map { it.id } shouldBe listOf(meldekortvedtak.id)

            tac.skalSendesTilMeldekortApi(sak.id) shouldBe false
        }
    }

    @Test
    fun `Sak tilMeldekortApiDTO og JSON-serialisering inneholder meldekortvedtak med dager`() {
        withTestApplicationContextAndPostgres { tac ->
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
            vedtakDto.erKorrigering shouldBe meldekortvedtak.harKorrigering
            vedtakDto.erAutomatiskBehandlet shouldBe meldekortvedtak.erAutomatiskBehandlet
            vedtakDto.meldeperiodebehandlinger shouldHaveSize meldekortvedtak.meldeperiodebehandlinger.size

            // Behandlinger må mappes parvis med riktig beregning (kjedeId-match)
            vedtakDto.meldeperiodebehandlinger.zip(meldekortvedtak.meldeperiodebehandlinger).forEach { (dto, behandling) ->
                dto.meldeperiodeId shouldBe behandling.meldeperiodeId.toString()
                dto.meldeperiodeKjedeId shouldBe behandling.kjedeId.toString()
                dto.brukersMeldekortId shouldBe behandling.brukersMeldekort.lastOrNull()?.id?.toString()
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

    /**
     * Leser flagget direkte fra databasen med egen SQL.
     * `skal_sendes_til_meldekort_api` skrives inn, men leses aldri ut igjen på domenemodellen — det er kun jobbens køspørring som bruker det.
     * Vi forurenser ikke [no.nav.tiltakspenger.saksbehandling.sak.Sak] med feltet bare for å gjøre det observerbart i test, og køspørringen er ingen god lesekanal for én sak.
     */
    private fun TestApplicationContext.skalSendesTilMeldekortApi(sakId: SakId): Boolean =
        (this.sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(
                queryOf(
                    "select skal_sendes_til_meldekort_api from sak where id = :id",
                    mapOf("id" to sakId.toString()),
                ).map { it.boolean("skal_sendes_til_meldekort_api") }.asSingle,
            )
        }!!
}
