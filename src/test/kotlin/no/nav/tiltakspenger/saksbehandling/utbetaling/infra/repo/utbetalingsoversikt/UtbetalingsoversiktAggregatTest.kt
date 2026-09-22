package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsfeiltype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsplan
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.OppdaterUtbetalingsoversiktJobb
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Køspørringen og kjøringen går på tvers av saker, så testene kjører isolert.
 * Sakene, utbetalingene og oppslagene bygges gjennom rutene og jobben.
 * Klokka starter 1. mai 2025, en fast helligdag på en torsdag: utbetalingene regnes som sendt fredag 2. mai, mandag 5. mai er ventedag, og de kan stå i reskontroen fra tirsdag 6. mai 06:10.
 */
class UtbetalingsoversiktAggregatTest {
    @Test
    @IsolatedDatabaseTest
    fun `sak velges først når en sendt utbetaling kan stå i reskontroen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen)
            val sak = sakMedOkUtbetaling(tac)

            spolTilÅpent(tac, 5.mai(2025))
            kø(tac) shouldBe emptyList()

            spolTilÅpent(tac, 6.mai(2025))
            kø(tac) shouldBe listOf(sak.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `sak der siste oppslag feilet velges først når ventetiden er ute`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = sakMedOkUtbetaling(tac)
            spolTilÅpent(tac, 6.mai(2025))
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(sak.fnr, tjenestefeil())
            jobb(tac).oppdaterForSak(sak.id)

            kø(tac) shouldBe emptyList()

            tac.clock.spol1timeFrem()
            kø(tac) shouldBe listOf(sak.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `sak med vellykket oppslag velges ikke igjen samme dag, men neste åpne dag`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = sakMedOkUtbetaling(tac)
            spolTilÅpent(tac, 6.mai(2025))
            jobb(tac).oppdaterForSak(sak.id)

            tac.clock.spolTil(6.mai(2025).atTime(20, 49))
            kø(tac) shouldBe emptyList()

            tac.clock.spolTil(7.mai(2025).atTime(6, 10))
            kø(tac) shouldBe listOf(sak.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `sak med lang frist velges likevel når den får en ny utbetaling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = sakMedOkUtbetaling(tac)
            spolTilÅpent(tac, 10.juni(2025))
            jobb(tac).oppdaterForSak(sak.id)
            spolTilÅpent(tac, 11.juni(2025))
            kø(tac) shouldBe emptyList()

            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                jobber = JobberEtterIverksettelse(journalførVedtaksbrev = false, distribuerVedtaksbrev = false),
            )
            spolTilÅpent(tac, 12.juni(2025))
            kø(tac) shouldBe emptyList()

            spolTilÅpent(tac, 13.juni(2025))
            kø(tac) shouldBe listOf(sak.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `køen har tidligste frist først og respekterer limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val medFeiletOppslag = sakMedOkUtbetaling(tac)
            val første = sakMedOkUtbetaling(tac)
            val andre = sakMedOkUtbetaling(tac)
            spolTilÅpent(tac, 6.mai(2025))
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(medFeiletOppslag.fnr, tjenestefeil())
            jobb(tac).oppdaterForSak(medFeiletOppslag.id)
            tac.clock.spol1timeFrem()

            kø(tac) shouldBe listOf(første.id, andre.id, medFeiletOppslag.id)
            kø(tac, limit = 2) shouldBe listOf(første.id, andre.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kjøringen fortsetter ved feil som gjelder saken og stopper ved feil som rammer alle`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val avvist = sakMedOkUtbetaling(tac)
            val tjenestenErNede = sakMedOkUtbetaling(tac)
            val ikkeForsøkt = sakMedOkUtbetaling(tac)
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(
                avvist.fnr,
                KunneIkkeHenteUtbetalingsoversikt.SakAvvist(ObjectMother.httpKlientUventetStatus(400)),
            )
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(tjenestenErNede.fnr, tjenestefeil())
            spolTilÅpent(tac, 6.mai(2025))

            jobb(tac).oppdaterUtbetalingsoversikter()

            feiltype(tac, avvist) shouldBe Oppslagsfeiltype.SAK_AVVIST
            feiltype(tac, tjenestenErNede) shouldBe Oppslagsfeiltype.TJENESTEFEIL
            status(tac, ikkeForsøkt) shouldBe Utbetalingsoversiktstatus.IkkeHentet
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kjøringen stopper når to oppslag har feilet`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val saker = List(3) { sakMedOkUtbetaling(tac) }
            saker.forEach {
                tac.utbetalingsoversiktFakeKlient.leggTilFeil(
                    it.fnr,
                    KunneIkkeHenteUtbetalingsoversikt.SakAvvist(ObjectMother.httpKlientUventetStatus(400)),
                )
            }
            spolTilÅpent(tac, 6.mai(2025))

            jobb(tac).oppdaterUtbetalingsoversikter()

            feiltype(tac, saker[0]) shouldBe Oppslagsfeiltype.SAK_AVVIST
            feiltype(tac, saker[1]) shouldBe Oppslagsfeiltype.SAK_AVVIST
            status(tac, saker[2]) shouldBe Utbetalingsoversiktstatus.IkkeHentet
        }
    }

    /** Når lagringen kaster, vet vi ikke hvorfor, og da skal ikke neste sak gi et nytt kall mot tjenesten. */
    @Test
    @IsolatedDatabaseTest
    fun `kjøringen stopper når lagringen kaster, uten å kalle tjenesten for neste sak`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val første = sakMedOkUtbetaling(tac)
            val andre = sakMedOkUtbetaling(tac)
            spolTilÅpent(tac, 6.mai(2025))
            val repoSomKasterVedLagring = object : UtbetalingsoversiktRepo by tac.utbetalingContext.utbetalingsoversiktRepo {
                override fun lagre(oversikt: Utbetalingsoversikt, metadata: UtbetalingsoversiktMetadata) = throw IllegalStateException("databasen svarer ikke")
            }

            OppdaterUtbetalingsoversiktJobb(
                sakRepo = tac.sakContext.sakRepo,
                utbetalingsoversiktRepo = repoSomKasterVedLagring,
                utbetalingsoversiktklient = tac.utbetalingsoversiktFakeKlient,
                clock = tac.clock,
                meterRegistry = SimpleMeterRegistry(),
                limit = 10,
            ).oppdaterUtbetalingsoversikter()

            tac.utbetalingsoversiktFakeKlient.sisteOppslagFor(første.fnr).shouldNotBeNull()
            tac.utbetalingsoversiktFakeKlient.sisteOppslagFor(andre.fnr) shouldBe null
        }
    }

    /** Statusen kan bli OK etter at saken er slått opp; da skal saken ikke vente ut den lange fristen. */
    @Test
    @IsolatedDatabaseTest
    fun `sak med lang frist velges når en utbetaling sendt før oppslaget får OK-kvittering`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen).first
            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                jobber = JobberEtterIverksettelse(oppdaterUtbetalingsstatus = false, journalførVedtaksbrev = false, distribuerVedtaksbrev = false),
            )
            spolTilÅpent(tac, 6.mai(2025))
            jobb(tac).oppdaterForSak(sak.id)
            val oversikt = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>().oversikt
            oversikt.plan shouldBe Oppslagsplan.etterVellykketOppslag(oversikt.hentet, harNyligSendtUtbetaling = false)

            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
            kø(tac) shouldBe emptyList()

            spolTilÅpent(tac, 7.mai(2025))
            kø(tac) shouldBe listOf(sak.id)
        }
    }

    /** Etter fem feil på rad er ventetiden en time; et forsøk etter stengetid flyttes til neste åpning. */
    @Test
    @IsolatedDatabaseTest
    fun `sak med nylig sendt utbetaling går ikke utenom ventetiden etter feil`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = sakMedOkUtbetaling(tac)
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(sak.fnr, tjenestefeil())
            tac.clock.spolTil(6.mai(2025).atTime(20, 0))
            repeat(5) { jobb(tac).oppdaterForSak(sak.id) }

            tac.clock.spolTil(7.mai(2025).atTime(6, 9))
            kø(tac) shouldBe emptyList()

            tac.clock.spolTil(7.mai(2025).atTime(6, 10))
            kø(tac) shouldBe listOf(sak.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kjøringen gjør ingenting når økonomisystemet er stengt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = sakMedOkUtbetaling(tac)
            tac.clock.spolTil(6.mai(2025))

            jobb(tac).oppdaterUtbetalingsoversikter()
            status(tac, sak) shouldBe Utbetalingsoversiktstatus.IkkeHentet

            spolTilÅpent(tac, 7.mai(2025))
            jobb(tac).oppdaterUtbetalingsoversikter()
            status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>()
        }
    }

    private fun jobb(tac: TestApplicationContext) = tac.utbetalingContext.oppdaterUtbetalingsoversiktJobb

    private fun kø(tac: TestApplicationContext, limit: Int = 10): List<SakId> =
        tac.utbetalingContext.utbetalingsoversiktRepo.hentSakerKlareForOppslag(nå = nå(tac.clock), limit = limit)

    private fun status(tac: TestApplicationContext, sak: Sak) = tac.utbetalingContext.utbetalingsoversiktRepo.hentStatusForSak(sak.id)

    private fun feiltype(tac: TestApplicationContext, sak: Sak) =
        status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagFeilet>().oversikt.feiltype

    private fun tjenestefeil() = KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil(ObjectMother.httpKlientUventetStatus(500))

    private fun spolTilÅpent(tac: TestApplicationContext, dato: LocalDate) {
        tac.clock.spolTil(dato.atTime(7, 0))
    }

    private suspend fun ApplicationTestBuilder.sakMedOkUtbetaling(tac: TestApplicationContext): Sak {
        val sak = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen).first
        opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            jobber = JobberEtterIverksettelse(journalførVedtaksbrev = false, distribuerVedtaksbrev = false),
        )
        return tac.sakContext.sakRepo.hentForSakId(sak.id)!!
    }
}
