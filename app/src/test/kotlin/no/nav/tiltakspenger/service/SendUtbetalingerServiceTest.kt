package no.nav.tiltakspenger.service

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.UtbetalingGateway
import no.nav.tiltakspenger.vedtak.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.vedtak.utbetaling.service.SendUtbetalingerService
import org.junit.jupiter.api.Test

internal class SendUtbetalingerServiceTest {
    private val utbetalingsvedtakRepo = mockk<UtbetalingsvedtakRepo>()
    private val utbetalingsklient = mockk<UtbetalingGateway>()
    private val sendUtbetalingerService = SendUtbetalingerService(utbetalingsvedtakRepo, utbetalingsklient)

    @Test
    fun `utbetaling blir iverksatt og markert som sendt til utbetaling`() = runTest {
        val utbetalingsvedtak = ObjectMother.utbetalingsvedtak()

        every { utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() } returns listOf(utbetalingsvedtak)
        val sendtUtbetaling = SendtUtbetaling("req", "res", 202)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Right(sendtUtbetaling)
        justRun { utbetalingsvedtakRepo.markerSendtTilUtbetaling(utbetalingsvedtak.id, any(), sendtUtbetaling) }

        sendUtbetalingerService.send()

        verify(exactly = 1) {
            utbetalingsvedtakRepo.markerSendtTilUtbetaling(
                utbetalingsvedtak.id,
                any(),
                sendtUtbetaling,
            )
        }
    }

    @Test
    fun `feilrespons fra utbetaling lagres`() = runTest {
        val utbetalingsvedtak = ObjectMother.utbetalingsvedtak()

        every { utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() } returns listOf(utbetalingsvedtak)
        val kunneIkkeUtbetale = KunneIkkeUtbetale("req", "res", 409)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Left(kunneIkkeUtbetale)
        justRun { utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(utbetalingsvedtak.id, kunneIkkeUtbetale) }

        sendUtbetalingerService.send()

        verify(exactly = 1) {
            utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(
                utbetalingsvedtak.id,
                kunneIkkeUtbetale,
            )
        }
    }
}
