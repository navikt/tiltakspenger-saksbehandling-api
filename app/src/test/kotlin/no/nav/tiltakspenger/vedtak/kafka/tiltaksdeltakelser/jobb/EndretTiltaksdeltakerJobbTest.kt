package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.jobb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.db.persisterSakOgSøknad
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mai
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.Oppgavebehov
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.getTiltaksdeltakerKafkaDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class EndretTiltaksdeltakerJobbTest {
    private val oppgaveGateway = mockk<OppgaveGateway>()
    private val oppgaveId = OppgaveId("50")

    @BeforeEach
    fun clearMockData() {
        clearMocks(oppgaveGateway)
        coEvery {
            oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(
                any(),
                Oppgavebehov.ENDRET_TILTAKDELTAKER,
            )
        } returns oppgaveId
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - ingen opprettet behandling - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(id = id),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(id = id, sakId = sak.id)
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null

                coVerify(exactly = 0) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - ingen iverksatt behandling - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                testDataHelper.persisterOpprettetFørstegangsbehandling(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(id = id),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(id = id, sakId = sak.id)
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, ingen endring - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            id = id,
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb =
                    getTiltaksdeltakerKafkaDb(id = id, sakId = sak.id, fom = deltakelseFom, tom = deltakelsesTom, dagerPerUke = 5F, deltakelsesprosent = 100F)
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, forlengelse - oppretter oppgave`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            id = id,
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusMonths(1),
                )
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, avbrutt - oppretter oppgave`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            id = id,
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = LocalDate.now(),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                )
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) { oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(any(), any()) }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        coEvery { oppgaveGateway.erFerdigstilt(any()) } returns false
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            id = id,
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = LocalDate.now(),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    oppgaveId = oppgaveId,
                    oppgaveSistSjekket = null,
                )
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprydning()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe LocalDateTime.now()
                    .truncatedTo(ChronoUnit.MINUTES)
                coVerify(exactly = 1) { oppgaveGateway.erFerdigstilt(oppgaveId) }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ferdigstilt - sletter fra db`() {
        coEvery { oppgaveGateway.erFerdigstilt(any()) } returns true
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveGateway)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattFørstegangsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            id = id,
                            deltakelseFom = deltakelseFom,
                            deltakelseTom = deltakelsesTom,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = LocalDate.now(),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    oppgaveId = oppgaveId,
                    oppgaveSistSjekket = LocalDateTime.now().minusHours(2),
                )
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding")

                endretTiltaksdeltakerJobb.opprydning()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 1) { oppgaveGateway.erFerdigstilt(oppgaveId) }
            }
        }
    }
}
