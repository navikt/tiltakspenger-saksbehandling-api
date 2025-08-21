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
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppdaterUtbetalingsstatusServiceTest {
    private val utbetalingsvedtakRepo = mockk<UtbetalingsvedtakRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val sendUtbetalingerService =
        OppdaterUtbetalingsstatusService(utbetalingsvedtakRepo, utbetalingsklient, fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `innhenting av status som ikke har fått ok etter mange forsøk blir ikke forsøkt på nytt med en gang`() =
        runTest {
            val utbetalingsvedtak = ObjectMother.utbetalingDetSkalHentesStatusFor(
                forsøkshistorikk = Forsøkshistorikk.opprett(
                    antallForsøk = 10,
                    forrigeForsøk = LocalDateTime.now(fixedClock).minusHours(23),
                    clock = fixedClock,
                ),
            )
            every { utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() } returns listOf(utbetalingsvedtak)

            sendUtbetalingerService.oppdaterUtbetalingsstatus()

            coVerify(exactly = 1) { utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() }
            coVerify(exactly = 0) { utbetalingsklient.hentUtbetalingsstatus(any()) }
            coVerify(exactly = 0) { utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(any(), any(), any()) }
        }

    @Test
    fun `innhenting av status som ikke har fått ok etter mange forsøkblir forsøkt på nytt etter en stund`() = runTest {
        val utbetalingsvedtak = ObjectMother.utbetalingDetSkalHentesStatusFor(
            forsøkshistorikk = Forsøkshistorikk.opprett(
                antallForsøk = 10,
                forrigeForsøk = LocalDateTime.now(fixedClock).minusHours(25),
                clock = fixedClock,
            ),
        )
        every { utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() } returns listOf(utbetalingsvedtak)
        coEvery { utbetalingsklient.hentUtbetalingsstatus(any()) } returns Either.Right(Utbetalingsstatus.Ok)
        coEvery {
            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                utbetalingsvedtak.vedtakId,
                Utbetalingsstatus.Ok,
                any(),
            )
        }

        sendUtbetalingerService.oppdaterUtbetalingsstatus()

        coVerify(exactly = 1) { utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() }
        coVerify(exactly = 1) { utbetalingsklient.hentUtbetalingsstatus(utbetalingsvedtak) }
        coVerify(exactly = 1) {
            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                utbetalingsvedtak.vedtakId,
                Utbetalingsstatus.Ok,
                any(),
            )
        }
    }
}
