package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SendUtbetalingerServiceTest {
    private val utbetalingRepo = mockk<UtbetalingRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val sendUtbetalingerService = SendUtbetalingerService(utbetalingRepo, utbetalingsklient, fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `utbetaling blir iverksatt og markert som sendt til utbetaling`() = runTest {
        val utbetaling = ObjectMother.utbetaling()

        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        val sendtUtbetaling = SendtUtbetaling("req", "res", 202)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Right(sendtUtbetaling)
        justRun { utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), sendtUtbetaling) }

        sendUtbetalingerService.send()

        verify(exactly = 1) {
            utbetalingRepo.markerSendtTilUtbetaling(
                utbetaling.id,
                any(),
                sendtUtbetaling,
            )
        }
    }

    @Test
    fun `feilrespons fra utbetaling lagres`() = runTest {
        val utbetaling = ObjectMother.utbetaling()

        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        val kunneIkkeUtbetale = KunneIkkeUtbetale("req", "res", 409)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Left(kunneIkkeUtbetale)
        justRun {
            utbetalingRepo.lagreFeilResponsFraUtbetaling(
                utbetalingId = utbetaling.id,
                utbetalingsrespons = kunneIkkeUtbetale,
            )
        }

        sendUtbetalingerService.send()

        verify(exactly = 1) {
            utbetalingRepo.lagreFeilResponsFraUtbetaling(
                utbetalingId = utbetaling.id,
                utbetalingsrespons = kunneIkkeUtbetale,
            )
        }
    }
}
