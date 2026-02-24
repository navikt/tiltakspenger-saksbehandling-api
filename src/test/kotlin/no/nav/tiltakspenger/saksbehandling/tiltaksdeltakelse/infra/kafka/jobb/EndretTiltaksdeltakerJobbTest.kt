package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattRevurderingStans
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetAutomatiskSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.getTiltaksdeltakerKafkaDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class EndretTiltaksdeltakerJobbTest {
    private val startRevurderingService = mockk<StartRevurderingService>()
    private val leggTilbakeBehandlingService = mockk<LeggTilbakeRammebehandlingService>()
    private val endretTiltaksdeltakerBehandlingService = EndretTiltaksdeltakerBehandlingService(startRevurderingService, leggTilbakeBehandlingService)
    private val oppgaveKlient = mockk<OppgaveKlient>()
    private val oppgaveId = OppgaveId("50")

    @BeforeEach
    fun clearMockData() {
        clearMocks(oppgaveKlient, startRevurderingService, leggTilbakeBehandlingService)
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
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
                testDataHelper.persisterSakOgSøknad(
                    fnr = fnr,
                    sak = sak,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
                        søknadstiltak = ObjectMother.søknadstiltak(
                            id = id,
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb =
                    getTiltaksdeltakerKafkaDb(id = id, sakId = sak.id, tiltaksdeltakerId = tiltaksdeltakerId)
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null

                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
                coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
                coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - ingen behandling for endret deltaker - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
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
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
                coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
                coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - åpen behandling for endret deltaker - oppretter oppgave, ikke revurdering`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = TikkendeKlokke()
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
                val deltakelseFom = LocalDate.now(clock).minusDays(2)
                val deltakelsesTom = LocalDate.now(clock).plusMonths(3)
                testDataHelper.persisterOpprettetSøknadsbehandling(
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                    fom = null,
                    tom = null,
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) {
                    oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                        any(),
                        any(),
                        "Deltakelsen er ikke aktuell.",
                    )
                }
                coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
                coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - åpen automatisk behandling for endret deltaker - oppdaterer venterTil, oppretter ikke oppgave`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = TikkendeKlokke()
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
                val deltakelseFom = LocalDate.now(clock).plusDays(2)
                val deltakelsesTom = LocalDate.now(clock).plusMonths(3)
                val (_, behandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling(
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    venterTil = deltakelseFom.atStartOfDay(),
                    frist = null,
                )
                val behandlingPaVent =
                    behandling.settPåVent(kommando = kommando, clock = testDataHelper.clock) as Søknadsbehandling
                behandlingRepo.lagre(behandlingPaVent)
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                    fom = null,
                    tom = null,
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
                coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
                coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
                behandlingRepo.hent(behandling.id).venterTil?.toLocalDate() shouldBe 1.januar(2025)
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, ingen endring - sletter fra db`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
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
                        tiltaksdeltakerId = tiltaksdeltakerId,
                    )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 0) { oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(any(), any(), any()) }
                coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
                coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, forlengelse, deltakelsesmengde - oppretter oppgave og revurdering`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val revurdering = ObjectMother.nyOpprettetRevurderingInnvilgelse(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = fnr,
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                )
                coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
                    sak,
                    revurdering,
                )
                coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
                    sak,
                    revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusMonths(1),
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

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
                coVerify(exactly = 1) { startRevurderingService.startRevurdering(match { it.sakId == sak.id && it.revurderingType == StartRevurderingType.INNVILGELSE }) }
                coVerify(exactly = 1) {
                    leggTilbakeBehandlingService.leggTilbakeBehandling(
                        sak.id,
                        any(),
                        AUTOMATISK_SAKSBEHANDLER,
                    )
                }
            }
        }
    }

    @Test
    fun `opprettOppgaveForEndredeDeltakere - iverksatt behandling, avbrutt - oppretter oppgave og revurdering`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val revurdering = ObjectMother.nyOpprettetRevurderingStans(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = fnr,
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                )
                coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
                    sak,
                    revurdering,
                )
                coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
                    sak,
                    revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.minusDays(2),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                coVerify(exactly = 1) {
                    oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                        any(),
                        any(),
                        "Deltakelsen er avbrutt.",
                    )
                }
                coVerify(exactly = 1) { startRevurderingService.startRevurdering(match { it.sakId == sak.id && it.revurderingType == StartRevurderingType.STANS }) }
                coVerify(exactly = 1) {
                    leggTilbakeBehandlingService.leggTilbakeBehandling(
                        sak.id,
                        any(),
                        AUTOMATISK_SAKSBEHANDLER,
                    )
                }
            }
        }
    }

    @Nested
    inner class `OpprettOppgaveForEndredeDeltakere - flere vedtak` {
        val clock = TikkendeKlokke()
        val fnr = Fnr.random()
        val sak = ObjectMother.nySak(fnr = fnr)
        private val førsteDeltakelseFom = 5.januar(2025)
        private val førsteDeltakelsesTom = 5.mai(2025)
        private val førsteSøknadstiltakId = UUID.randomUUID().toString()
        private val forsteTiltaksdeltakerId = TiltaksdeltakerId.random()
        private val førsteSøknadId = SøknadId.random()
        private val førsteSøknad = ObjectMother.nyInnvilgbarSøknad(
            clock = clock,
            id = førsteSøknadId,
            personopplysninger = ObjectMother.personSøknad(fnr = fnr),
            søknadstiltak = ObjectMother.søknadstiltak(
                id = førsteSøknadstiltakId,
                deltakelseFom = førsteDeltakelseFom,
                deltakelseTom = førsteDeltakelsesTom,
                tiltaksdeltakerId = forsteTiltaksdeltakerId,
            ),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        )
        private val andreDeltakelseFom = 10.mai(2025)
        private val andreDeltakelsesTom = 11.juni(2025)
        private val andreSøknadstiltakId = UUID.randomUUID().toString()
        private val andreTiltaksdeltakerId = TiltaksdeltakerId.random()
        private val andreSøknadId = SøknadId.random()
        private val andreSøknad = ObjectMother.nyInnvilgbarSøknad(
            clock = clock,
            id = andreSøknadId,
            personopplysninger = ObjectMother.personSøknad(fnr = fnr),
            søknadstiltak = ObjectMother.søknadstiltak(
                id = andreSøknadstiltakId,
                deltakelseFom = andreDeltakelseFom,
                deltakelseTom = andreDeltakelsesTom,
                tiltaksdeltakerId = andreTiltaksdeltakerId,
            ),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        )
        private val førsteTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            id = førsteSøknadstiltakId,
            sakId = sak.id,
            fom = førsteDeltakelseFom,
            tom = LocalDate.now(clock),
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
            tiltaksdeltakerId = forsteTiltaksdeltakerId,
        )
        private val andreTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
            id = andreSøknadstiltakId,
            sakId = sak.id,
            fom = andreDeltakelseFom,
            tom = LocalDate.now(clock),
            deltakerstatus = TiltakDeltakerstatus.Avbrutt,
            tiltaksdeltakerId = andreTiltaksdeltakerId,
        )

        @Test
        fun `innvilgelse + stans (over hele perioden) lager ikke oppgave`() {
            withTestApplicationContext { }
            withMigratedDb(runIsolated = true) { testDataHelper ->
                runBlocking {
                    val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                    val sakRepo = testDataHelper.sakRepo
                    val behandlingRepo = testDataHelper.behandlingRepo
                    val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                        tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                        sakRepo = sakRepo,
                        oppgaveKlient = oppgaveKlient,
                        rammebehandlingRepo = behandlingRepo,
                        clock = testDataHelper.clock,
                        endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                    )
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
                    )
                    val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                        id = førsteSøknadstiltakId,
                        sakId = sak.id,
                        fom = førsteDeltakelseFom,
                        tom = LocalDate.now(clock),
                        deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                        tiltaksdeltakerId = forsteTiltaksdeltakerId,
                    )

                    tiltaksdeltakerKafkaRepository.lagre(
                        tiltaksdeltakerKafkaDb,
                        "melding",
                        nå(testDataHelper.clock).minusMinutes(20),
                    )
                    endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                    val oppdatertTiltaksdeltakerKafkaDb =
                        tiltaksdeltakerKafkaRepository.hent(førsteSøknad.id.toString())
                    oppdatertTiltaksdeltakerKafkaDb shouldBe null
                    coVerify(exactly = 0) {
                        oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                            any(),
                            any(),
                            "Deltakelsen er avbrutt.",
                        )
                    }
                    coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
                    coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
                }
            }
        }

        @Test
        fun `innvilgelse - avslag lager oppgave for innvilget`() {
            withMigratedDb(runIsolated = true) { testDataHelper ->
                runBlocking {
                    val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                    val sakRepo = testDataHelper.sakRepo
                    val behandlingRepo = testDataHelper.behandlingRepo
                    val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                        tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                        sakRepo = sakRepo,
                        oppgaveKlient = oppgaveKlient,
                        rammebehandlingRepo = behandlingRepo,
                        clock = testDataHelper.clock,
                        endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                    )
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
                    val revurdering = ObjectMother.nyOpprettetRevurderingStans(
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        fnr = fnr,
                        saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    )
                    coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
                        sak,
                        revurdering,
                    )
                    coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
                        sak,
                        revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
                    )

                    tiltaksdeltakerKafkaRepository.lagre(
                        førsteTiltaksdeltakerKafkaDb,
                        "melding",
                        nå(testDataHelper.clock).minusMinutes(20),
                    )
                    tiltaksdeltakerKafkaRepository.lagre(
                        andreTiltaksdeltakerKafkaDb,
                        "melding",
                        nå(testDataHelper.clock).minusMinutes(20),
                    )

                    endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                    val førsteOppdatertTiltaksdeltakerKafkaDb =
                        tiltaksdeltakerKafkaRepository.hent(førsteSøknadstiltakId)
                    førsteOppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                    førsteOppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId

                    val andreOppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(andreSøknadstiltakId)
                    andreOppdatertTiltaksdeltakerKafkaDb shouldBe null

                    coVerify(exactly = 1) {
                        oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                            any(),
                            any(),
                            "Deltakelsen er avbrutt.",
                        )
                    }
                    coVerify(exactly = 1) { startRevurderingService.startRevurdering(match { it.sakId == sak.id && it.revurderingType == StartRevurderingType.STANS }) }
                    coVerify(exactly = 1) {
                        leggTilbakeBehandlingService.leggTilbakeBehandling(
                            sak.id,
                            any(),
                            AUTOMATISK_SAKSBEHANDLER,
                        )
                    }
                }
            }
        }

        @Test
        fun `avslag - innvilgelse lager oppgave for innvilget`() {
            withMigratedDb(runIsolated = true) { testDataHelper ->
                runBlocking {
                    val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                    val sakRepo = testDataHelper.sakRepo
                    val behandlingRepo = testDataHelper.behandlingRepo
                    val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                        tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                        sakRepo = sakRepo,
                        oppgaveKlient = oppgaveKlient,
                        rammebehandlingRepo = behandlingRepo,
                        clock = testDataHelper.clock,
                        endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                    )
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
                    val revurdering = ObjectMother.nyOpprettetRevurderingStans(
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        fnr = fnr,
                        saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    )
                    coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
                        sak,
                        revurdering,
                    )
                    coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
                        sak,
                        revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
                    )

                    tiltaksdeltakerKafkaRepository.lagre(
                        førsteTiltaksdeltakerKafkaDb,
                        "melding",
                        nå(testDataHelper.clock).minusMinutes(20),
                    )
                    tiltaksdeltakerKafkaRepository.lagre(
                        andreTiltaksdeltakerKafkaDb,
                        "melding",
                        nå(testDataHelper.clock).minusMinutes(20),
                    )

                    endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()

                    val førsteOppdatertTiltaksdeltakerKafkaDb =
                        tiltaksdeltakerKafkaRepository.hent(førsteSøknadstiltakId)
                    førsteOppdatertTiltaksdeltakerKafkaDb shouldBe null

                    val andreOppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(andreSøknadstiltakId)
                    andreOppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                    andreOppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId

                    coVerify(exactly = 1) {
                        oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                            any(),
                            any(),
                            "Deltakelsen er avbrutt.",
                        )
                    }
                    coVerify(exactly = 1) { startRevurderingService.startRevurdering(match { it.sakId == sak.id && it.revurderingType == StartRevurderingType.STANS }) }
                    coVerify(exactly = 1) {
                        leggTilbakeBehandlingService.leggTilbakeBehandling(
                            sak.id,
                            any(),
                            AUTOMATISK_SAKSBEHANDLER,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        coEvery { oppgaveKlient.erFerdigstilt(any()) } returns false
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val clock = testDataHelper.clock
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    clock = clock,
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = LocalDate.now(clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    oppgaveId = oppgaveId,
                    oppgaveSistSjekket = null,
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprydning()

                val oppdatertTiltaksdeltakerKafkaDb = tiltaksdeltakerKafkaRepository.hent(id)
                oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe oppgaveId
                oppdatertTiltaksdeltakerKafkaDb?.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe nå(
                    testDataHelper.clock,
                )
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
                val clock = testDataHelper.clock
                val tiltaksdeltakerKafkaRepository = testDataHelper.tiltaksdeltakerKafkaRepository
                val sakRepo = testDataHelper.sakRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val endretTiltaksdeltakerJobb = EndretTiltaksdeltakerJobb(
                    tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
                    sakRepo = sakRepo,
                    oppgaveKlient = oppgaveKlient,
                    rammebehandlingRepo = behandlingRepo,
                    clock = testDataHelper.clock,
                    endretTiltaksdeltakerBehandlingService = endretTiltaksdeltakerBehandlingService,
                )
                val id = UUID.randomUUID().toString()
                val fnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = fnr)
                val tiltaksdeltakerId = TiltaksdeltakerId.random()
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                testDataHelper.persisterIverksattSøknadsbehandling(
                    clock = clock,
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
                            tiltaksdeltakerId = tiltaksdeltakerId,
                        ),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = id,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = LocalDate.now(clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    oppgaveId = oppgaveId,
                    oppgaveSistSjekket = nå(testDataHelper.clock).minusHours(2),
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(testDataHelper.clock).minusMinutes(20),
                )

                endretTiltaksdeltakerJobb.opprydning()

                tiltaksdeltakerKafkaRepository.hent(id) shouldBe null
                coVerify(exactly = 1) { oppgaveKlient.erFerdigstilt(oppgaveId) }
            }
        }
    }
}
