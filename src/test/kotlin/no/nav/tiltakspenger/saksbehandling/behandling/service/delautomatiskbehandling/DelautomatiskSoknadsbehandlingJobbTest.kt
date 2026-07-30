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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
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
    fun `opprettSøknadsbehandlingerFraNyeSøknader - oppretter behandling for åpen søknad uten behandling`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
                )

                val soknad = testDataHelper.persisterSakOgSøknad()
                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()
                coEvery {
                    startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                        any(),
                        any(),
                    )
                } returns ObjectMother.nyOpprettetAutomatiskSøknadsbehandling().right()

                delautomatiskSoknadsbehandlingJobb.opprettSøknadsbehandlingForSøknad(soknad.id)

                coVerify { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(soknad, any()) }
            }
        }
    }

    @Test
    fun `opprettSøknadsbehandlingerFraNyeSøknader - oppretter ikke behandling for avbrutt søknad`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
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

                delautomatiskSoknadsbehandlingJobb.opprettSøknadsbehandlingForSøknad(soknad.id)

                coVerify(exactly = 0) { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(any(), any()) }
            }
        }
    }

    @Test
    fun `opprettSøknadsbehandlingerFraNyeSøknader - oppretter ikke behandling for søknad med åpen behandling`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
                )

                val (_, behandling, _) = testDataHelper.persisterOpprettetSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.opprettSøknadsbehandlingForSøknad(behandling.søknad.id)

                coVerify(exactly = 0) { startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(any(), any()) }
            }
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandlinger - behandler opprettet automatisk behandling`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
                )

                val (_, automatiskBehandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(automatiskBehandling.id)

                coVerify { delautomatiskBehandlingService.behandleAutomatisk(automatiskBehandling, any()) }
                coVerify(exactly = 0) {
                    oppdaterSaksopplysningerService.oppdaterSaksopplysninger(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
            }
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandlinger - behandler ikke behandling med status UNDER_BEHANDLING`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
                )

                val (_, behandling, _) = testDataHelper.persisterOpprettetSøknadsbehandling()

                delautomatiskSoknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)

                coVerify(exactly = 0) { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandlinger - behandler ikke automatisk behandling der venter til ikke er passert`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
                )

                val (_, automatiskBehandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling()
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = automatiskBehandling.sakId,
                    rammebehandlingId = automatiskBehandling.id,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    venterTil = nå(testDataHelper.clock).plusDays(1),
                    frist = null,
                )
                val behandlingPaVent = automatiskBehandling.settPåVent(
                    kommando = kommando,
                    clock = testDataHelper.clock,
                ).first as Søknadsbehandling
                behandlingRepo.lagre(behandlingPaVent)

                delautomatiskSoknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandlingPaVent.id)

                coVerify(exactly = 0) { delautomatiskBehandlingService.behandleAutomatisk(any(), any()) }
            }
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandlinger - behandler automatisk behandling der venter til er passert, oppdaterer saksopplysninger`() {
        withMigratedDb { testDataHelper ->
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
                    testDataHelper.clock,
                )

                val (sak, automatiskBehandling, _) = testDataHelper.persisterOpprettetAutomatiskSøknadsbehandling()
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = automatiskBehandling.sakId,
                    rammebehandlingId = automatiskBehandling.id,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    venterTil = nå(testDataHelper.clock).minusDays(1),
                    frist = null,
                )
                val behandlingPaVent = automatiskBehandling.settPåVent(
                    kommando = kommando,
                    clock = testDataHelper.clock,
                ).first as Søknadsbehandling
                behandlingRepo.lagre(behandlingPaVent)
                coEvery {
                    oppdaterSaksopplysningerService.oppdaterSaksopplysninger(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns (sak to behandlingPaVent).right()

                delautomatiskSoknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandlingPaVent.id)

                coVerify {
                    delautomatiskBehandlingService.behandleAutomatisk(
                        match { it.id == behandlingPaVent.id },
                        any(),
                    )
                }
                coVerify {
                    oppdaterSaksopplysningerService.oppdaterSaksopplysninger(
                        automatiskBehandling.sakId,
                        automatiskBehandling.id,
                        AUTOMATISK_SAKSBEHANDLER,
                        any(),
                    )
                }
            }
        }
    }
}
