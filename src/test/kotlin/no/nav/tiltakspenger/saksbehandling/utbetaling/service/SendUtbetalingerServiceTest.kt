package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import org.junit.jupiter.api.Test

class SendUtbetalingerServiceTest {
    @Test
    fun `utbetaling blir iverksatt og markert som sendt til utbetaling`() = runTest {
        val utbetalingRepo = mockk<UtbetalingRepo>()
        val utbetalingsklient = mockk<Utbetalingsklient>()
        val sendUtbetalingerService =
            SendUtbetalingerService(utbetalingRepo, utbetalingsklient, mockk<StatistikkService>(), fixedClock)
        val utbetaling = ObjectMother.utbetaling()

        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        val sendtUtbetaling = SendtUtbetaling("req", "res", 202, alleredeMottattTidligere = false)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Right(sendtUtbetaling)
        justRun { utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), sendtUtbetaling) }

        sendUtbetalingerService.sendUtbetalingerTilHelved()

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
        val utbetalingRepo = mockk<UtbetalingRepo>()
        val utbetalingsklient = mockk<Utbetalingsklient>()
        val sendUtbetalingerService =
            SendUtbetalingerService(utbetalingRepo, utbetalingsklient, mockk<StatistikkService>(), fixedClock)
        val utbetaling = ObjectMother.utbetaling()

        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        val kunneIkkeUtbetale = KunneIkkeUtbetale(request = "req", feil = ObjectMother.httpKlientUventetStatus(statusCode = 409, body = "res"))
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Left(kunneIkkeUtbetale)
        justRun {
            utbetalingRepo.lagreFeilResponsFraUtbetaling(
                utbetalingId = utbetaling.id,
                utbetalingsrespons = kunneIkkeUtbetale,
            )
        }

        sendUtbetalingerService.sendUtbetalingerTilHelved()

        verify(exactly = 1) {
            utbetalingRepo.lagreFeilResponsFraUtbetaling(
                utbetalingId = utbetaling.id,
                utbetalingsrespons = kunneIkkeUtbetale,
            )
        }
    }
}
