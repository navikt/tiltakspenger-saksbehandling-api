package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.jobb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattRevurderingStans
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository.getTiltaksdeltakerKafkaDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class EndretTiltaksdeltakerJobbTest {
    private val oppgaveKlient = mockk<OppgaveKlient>()
    private val oppgaveId = OppgaveId("50")

    @BeforeEach
    fun clearMockData() {
        clearMocks(oppgaveKlient)
        coEvery {
            oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                any(),
                Oppgavebehov.ENDRET_TILTAKDELTAKER,
                any(),
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
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(id = id),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(id = id, sakId = sak.id)
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null

                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
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
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                testDataHelper.persisterOpprettetSøknadsbehandling(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(id = id, sakId = sak.id)
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
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
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    getTiltaksdeltakerKafkaDb(
                        id = id,
                        sakId = sak.id,
                        fom = deltakelseFom,
                        tom = deltakelsesTom,
                        dagerPerUke = 5F,
                        deltakelsesprosent = 100F,
                    )
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, forlengelse, deltakelsesmengde - oppretter oppgave`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) {
                    oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                        any(),
                        any(),
                        "- Endret deltakelsesmengde\n" +
                            "- Deltakelsen har blitt forlenget",
                    )
                }
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
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                    tom = deltakelsesTom.minusDays(2),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                )
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), "Deltakelsen er avbrutt.") }
            }
        }
    }

    @Nested
    inner class `OpprettOppgaveForEndredeDeltakere - flere vedtak` {
        val fnr = Fnr.random()
        val sak = ObjectMother.nySak(fnr = fnr)
        private val førsteDeltakelseFom = 5.januar(2025)
        private val førsteDeltakelsesTom = 5.mai(2025)
        private val førsteSøknadstiltakId = UUID.randomUUID().toString()
        private val førsteSøknadId = SøknadId.random()
        private val førsteSøknad = ObjectMother.nyInnvilgbarSøknad(
            id = førsteSøknadId,
            personopplysninger = ObjectMother.personSøknad(fnr = fnr),
            søknadstiltak = ObjectMother.søknadstiltak(
                id = førsteSøknadstiltakId,
                deltakelseFom = førsteDeltakelseFom,
                deltakelseTom = førsteDeltakelsesTom,
            ),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        )
        private val andreDeltakelseFom = 10.mai(2025)
        private val andreDeltakelsesTom = 11.juni(2025)
        private val andreSøknadstiltakId = UUID.randomUUID().toString()
        private val andreSøknadId = SøknadId.random()
        private val andreSøknad = ObjectMother.nyInnvilgbarSøknad(
            id = andreSøknadId,
            personopplysninger = ObjectMother.personSøknad(fnr = fnr),
            søknadstiltak = ObjectMother.søknadstiltak(
                id = andreSøknadstiltakId,
                deltakelseFom = andreDeltakelseFom,
                deltakelseTom = andreDeltakelsesTom,
            ),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        )
        private val førsteTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            id = førsteSøknadstiltakId,
            sakId = sak.id,
            fom = førsteDeltakelseFom,
            tom = LocalDate.now(),
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
        )
        private val andreTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            id = andreSøknadstiltakId,
            sakId = sak.id,
            fom = andreDeltakelseFom,
            tom = LocalDate.now(),
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
        )

        @Test
        fun `innvilgelse + stans (over hele perioden) lager ikke oppgave`() {
            withMigratedDb(runIsolated = true) { testDataHelper ->
                runBlocking {
                    val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                    val sakRepo = testDataHelper.sakRepo
                    val endretTiltaksdeltakerJobb =
                        EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                    val (sakMedFørstegangsvedtak, vedtak) = testDataHelper.persisterIverksattSøknadsbehandling(
                        sakId = sak.id,
                        fnr = fnr,
                        deltakelseFom = førsteDeltakelseFom,
                        deltakelseTom = førsteDeltakelsesTom,
                        søknadId = førsteSøknadId,
                        sak = sak,
                        søknad = førsteSøknad,
                    )
                    testDataHelper.persisterIverksattRevurderingStans(
                        sak = sakMedFørstegangsvedtak,
                        stansFraOgMed = vedtak.fraOgMed,
                        stansTilOgMed = vedtak.tilOgMed,
                    )
                    val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                        id = førsteSøknadstiltakId,
                        sakId = sak.id,
                        fom = førsteDeltakelseFom,
                        tom = LocalDate.now(),
                        deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    )

                    tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))
                    endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                    val oppdatertTiltaksdeltakerKafkaDb =
                        tiltaksdeltakerKafkaRepository.hent(førsteSøknad.id.toString())
                    oppdatertTiltaksdeltakerKafkaDb shouldBe null
                    coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), "Deltakelsen er avbrutt.") }
                }
            }
        }

        @Test
        fun `innvilgelse - avslag lager oppgave for innvilget`() {
            withMigratedDb(runIsolated = true) { testDataHelper ->
                runBlocking {
                    val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                    val sakRepo = testDataHelper.sakRepo
                    val endretTiltaksdeltakerJobb =
                        EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                    val (sakMedFørstegangsvedtak) = testDataHelper.persisterIverksattSøknadsbehandling(
                        sakId = sak.id,
                        fnr = fnr,
                        deltakelseFom = førsteDeltakelseFom,
                        deltakelseTom = førsteDeltakelsesTom,
                        søknadId = førsteSøknadId,
                        sak = sak,
                        søknad = førsteSøknad,
                    )
                    testDataHelper.persisterRammevedtakAvslag(
                        sakId = sakMedFørstegangsvedtak.id,
                        fnr = fnr,
                        deltakelseFom = andreDeltakelseFom,
                        deltakelseTom = andreDeltakelsesTom,
                        søknadId = andreSøknadId,
                        sak = sakMedFørstegangsvedtak,
                        søknad = andreSøknad,
                    )

                    tiltaksdeltakerKafkaRepository.lagre(førsteTiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))
                    tiltaksdeltakerKafkaRepository.lagre(andreTiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                    endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                    val førsteOppdatertTiltaksdeltakerKafkaDb =
                        tiltaksdeltakerKafkaRepository.hent(førsteSøknadstiltakId)
                    førsteOppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                    førsteOppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId

                    val andreOppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(andreSøknadstiltakId)
                    andreOppdatertTiltaksdeltakerKafkaDb shouldBe null

                    coVerify(exactly = 1) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), "Deltakelsen er avbrutt.") }
                }
            }
        }

        @Test
        fun `avslag - innvilgelse lager oppgave for innvilget`() {
            withMigratedDb(runIsolated = true) { testDataHelper ->
                runBlocking {
                    val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                    val sakRepo = testDataHelper.sakRepo
                    val endretTiltaksdeltakerJobb =
                        EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                    val (sakMedFørstegangsvedtak) = testDataHelper.persisterRammevedtakAvslag(
                        sakId = sak.id,
                        fnr = fnr,
                        deltakelseFom = førsteDeltakelseFom,
                        deltakelseTom = førsteDeltakelsesTom,
                        søknadId = førsteSøknadId,
                        sak = sak,
                        søknad = førsteSøknad,
                    )
                    testDataHelper.persisterIverksattSøknadsbehandling(
                        sakId = sakMedFørstegangsvedtak.id,
                        fnr = fnr,
                        deltakelseFom = andreDeltakelseFom,
                        deltakelseTom = andreDeltakelsesTom,
                        søknadId = andreSøknadId,
                        sak = sakMedFørstegangsvedtak,
                        søknad = andreSøknad,
                    )

                    tiltaksdeltakerKafkaRepository.lagre(førsteTiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))
                    tiltaksdeltakerKafkaRepository.lagre(andreTiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                    endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                    val førsteOppdatertTiltaksdeltakerKafkaDb =
                        tiltaksdeltakerKafkaRepository.hent(førsteSøknadstiltakId)
                    førsteOppdatertTiltaksdeltakerKafkaDb shouldBe null

                    val andreOppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(andreSøknadstiltakId)
                    andreOppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                    andreOppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId

                    coVerify(exactly = 1) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), "Deltakelsen er avbrutt.") }
                }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        coEvery { oppgaveKlient.erFerdigstilt(any()) } returns false
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprydning()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe LocalDateTime.now()
                    .truncatedTo(ChronoUnit.MINUTES)
                coVerify(exactly = 1) { oppgaveKlient.erFerdigstilt(oppgaveId) }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ferdigstilt - sletter fra db`() {
        coEvery { oppgaveKlient.erFerdigstilt(any()) } returns true
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val endretTiltaksdeltakerJobb =
                    EndretTiltaksdeltakerJobb(tiltaksdeltakerKafkaRepository, sakRepo, oppgaveKlient)
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    sakId = sak.id,
                    fnr = fnr,
                    deltakelseFom = deltakelseFom,
                    deltakelseTom = deltakelsesTom,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
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
                tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, "melding", LocalDateTime.now().minusMinutes(20))

                endretTiltaksdeltakerJobb.opprydning()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 1) { oppgaveKlient.erFerdigstilt(oppgaveId) }
            }
        }
    }
}
