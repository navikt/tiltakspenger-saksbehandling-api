package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppdaterUtbetalingsstatusServiceTest {
    private val utbetalingRepo = mockk<UtbetalingRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val sendUtbetalingerService =
        OppdaterUtbetalingsstatusService(utbetalingRepo, utbetalingsklient, fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `innhenting av status som ikke har fått ok etter mange forsøk blir ikke forsøkt på nytt med en gang`() =
        runTest {
            val utbetaling = ObjectMother.utbetalingDetSkalHentesStatusFor(
                forsøkshistorikk = Forsøkshistorikk.opprett(
                    antallForsøk = 10,
                    forrigeForsøk = LocalDateTime.now(fixedClock).minusHours(23),
                    clock = fixedClock,
                ),
            )
            every { utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() } returns listOf(utbetaling)

            sendUtbetalingerService.oppdaterUtbetalingsstatus()

            coVerify(exactly = 1) { utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() }
            coVerify(exactly = 0) { utbetalingsklient.hentUtbetalingsstatus(any()) }
            coVerify(exactly = 0) { utbetalingRepo.oppdaterUtbetalingsstatus(any(), any(), any()) }
        }

    @Test
    fun `innhenting av status som ikke har fått ok etter mange forsøkblir forsøkt på nytt etter en stund`() = runTest {
        val utbetaling = ObjectMother.utbetalingDetSkalHentesStatusFor(
            forsøkshistorikk = Forsøkshistorikk.opprett(
                antallForsøk = 10,
                forrigeForsøk = LocalDateTime.now(fixedClock).minusHours(25),
                clock = fixedClock,
            ),
        )
        every { utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() } returns listOf(utbetaling)
        coEvery { utbetalingsklient.hentUtbetalingsstatus(any()) } returns Either.Right(Utbetalingsstatus.Ok)
        coEvery {
            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetaling.utbetalingId,
                Utbetalingsstatus.Ok,
                any(),
            )
        }

        sendUtbetalingerService.oppdaterUtbetalingsstatus()

        coVerify(exactly = 1) { utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() }
        coVerify(exactly = 1) { utbetalingsklient.hentUtbetalingsstatus(utbetaling) }
        coVerify(exactly = 1) {
            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetaling.utbetalingId,
                Utbetalingsstatus.Ok,
                any(),
            )
        }
    }
}
