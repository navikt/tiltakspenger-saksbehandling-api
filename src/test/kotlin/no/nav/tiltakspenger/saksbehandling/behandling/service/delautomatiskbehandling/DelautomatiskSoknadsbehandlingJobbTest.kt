package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.right
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetAutomatiskSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import org.junit.jupiter.api.Test

class DelautomatiskSoknadsbehandlingJobbTest {
    @Test
    fun `opprettBehandlingForNyeSoknader - oppretter behandling for åpen søknad uten behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                val soknad = testDataHelper.persisterSakOgSøknad()
                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()
                coEvery {
                    startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                        any(),
                        any(),
                    )
                } returns ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.opprettBehandlingForNyeSoknader()

                coVerify { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(soknad, any()) }
            }
        }
    }

    @Test
    fun `opprettBehandlingForNyeSoknader - oppretter ikke behandling for avbrutt søknad`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                val soknad = testDataHelper.persisterSakOgSøknad()

                soknad as InnvilgbarSøknad
                soknadRepo.lagreAvbruttSøknad(
                    soknad.copy(
                        avbrutt = Avbrutt(
                            nå(testDataHelper.clock),
                            "saksbehandler",
                            "begrunnelse".toNonBlankString(),
                        ),
                    ),
                )

                delautomatiskSoknadsbehandlingJobb.opprettBehandlingForNyeSoknader()

                coVerify(exactly = 0) { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettBehandlingForNyeSoknader - oppretter ikke behandling for søknad med åpen behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                testDataHelper.persisterOpprettetSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.opprettBehandlingForNyeSoknader()

                coVerify(exactly = 0) { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(any(), any()) }
            }
        }
    }

    @Test
    fun `behandleSoknaderAutomatisk - behandler opprettet automatisk behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                val (_, automatiskBehandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.behandleSoknaderAutomatisk()

                coVerify { delautomatiskBehandlingService.behandleAutomatisk(automatiskBehandling, any()) }
                coVerify(exactly = 0) { oppdaterSaksopplysningerService.oppdaterSaksopplysninger(any(), any(), any(), any()) }
            }
        }
    }

    @Test
    fun `behandleSoknaderAutomatisk - behandler ikke behandling med status UNDER_BEHANDLING`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                delautomatiskSoknadsbehandlingJobb.behandleSoknaderAutomatisk()

                coVerify(exactly = 0) { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }

    @Test
    fun `behandleSoknaderAutomatisk - behandler ikke automatisk behandling der venter til ikke er passert`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                val (_, automatiskBehandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling()
                val behandlingPaVent = automatiskBehandling.settPåVent(
                    endretAv = AUTOMATISK_SAKSBEHANDLER,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    clock = testDataHelper.clock,
                    venterTil = nå(testDataHelper.clock).plusDays(1),
                ) as Søknadsbehandling
                behandlingRepo.lagre(behandlingPaVent)

                delautomatiskSoknadsbehandlingJobb.behandleSoknaderAutomatisk()

                coVerify(exactly = 0) { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }

    @Test
    fun `behandleSoknaderAutomatisk - behandler automatisk behandling der venter til er passert, oppdaterer saksopplysninger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val behandlingRepo = testDataHelper.behandlingRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val oppdaterSaksopplysningerService = mockk<OppdaterSaksopplysningerService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    behandlingRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                    oppdaterSaksopplysningerService,
                )

                val (sak, automatiskBehandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling()
                val behandlingPaVent = automatiskBehandling.settPåVent(
                    endretAv = AUTOMATISK_SAKSBEHANDLER,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    clock = testDataHelper.clock,
                    venterTil = nå(testDataHelper.clock).minusDays(1),
                ) as Søknadsbehandling
                behandlingRepo.lagre(behandlingPaVent)
                coEvery { oppdaterSaksopplysningerService.oppdaterSaksopplysninger(any(), any(), any(), any()) } returns (sak to behandlingPaVent).right()

                delautomatiskSoknadsbehandlingJobb.behandleSoknaderAutomatisk()

                coVerify { delautomatiskBehandlingService.behandleAutomatisk(match { it.id == behandlingPaVent.id }, any()) }
                coVerify { oppdaterSaksopplysningerService.oppdaterSaksopplysninger(automatiskBehandling.sakId, automatiskBehandling.id, AUTOMATISK_SAKSBEHANDLER, any()) }
            }
        }
    }
}
