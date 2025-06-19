package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DelautomatiskSoknadsbehandlingJobbTest {
    @Test
    fun `oppretter behandling for åpen søknad uten behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                )

                val soknad = testDataHelper.persisterSakOgSøknad()
                coEvery {
                    startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                        any(),
                        any(),
                    )
                } returns ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.behandleNyeSoknader()

                coVerify { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(soknad, any()) }
                coVerify { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }

    @Test
    fun `oppretter ikke behandling for avbrutt søknad`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                )

                val soknad = testDataHelper.persisterSakOgSøknad()
                soknadRepo.lagreAvbruttSøknad(
                    soknad.copy(
                        avbrutt = Avbrutt(
                            LocalDateTime.now(),
                            "saksbehandler",
                            "begrunnelse",
                        ),
                    ),
                )

                delautomatiskSoknadsbehandlingJobb.behandleNyeSoknader()

                coVerify(exactly = 0) { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(any(), any()) }
                coVerify(exactly = 0) { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }

    @Test
    fun `oppretter ikke behandling for søknad med åpen behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val soknadRepo = testDataHelper.søknadRepo
                val startSøknadsbehandlingService = mockk<StartSøknadsbehandlingService>()
                val delautomatiskBehandlingService = mockk<DelautomatiskBehandlingService>(relaxed = true)
                val delautomatiskSoknadsbehandlingJobb = DelautomatiskSoknadsbehandlingJobb(
                    soknadRepo,
                    startSøknadsbehandlingService,
                    delautomatiskBehandlingService,
                )

                testDataHelper.persisterOpprettetSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.behandleNyeSoknader()

                coVerify(exactly = 0) { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(any(), any()) }
                coVerify(exactly = 0) { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }
}
