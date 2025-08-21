package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SendUtbetalingerServiceTest {
    private val utbetalingsvedtakRepo = mockk<UtbetalingsvedtakRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val sendUtbetalingerService = SendUtbetalingerService(utbetalingsvedtakRepo, utbetalingsklient, fixedClock)

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

    @Test
    fun `utbetaling som har feilet mange ganger blir ikke forsøkt på nytt med en gang`() = runTest {
        val utbetalingsvedtak = ObjectMother.utbetalingsvedtak(
            sendtTilUtbetaling = LocalDateTime.now(fixedClock).minusHours(23),
            status = Utbetalingsstatus.SendtTilOppdrag,
            statusMetadata = Forsøkshistorikk(
                antallForsøk = 10,
                forrigeForsøk = LocalDateTime.now(fixedClock),
            ),
        )
        every { utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() } returns listOf(utbetalingsvedtak)

        sendUtbetalingerService.send()

        coVerify(exactly = 0) {
            utbetalingsklient.iverksett(any(), any(), any())
        }
    }

    @Test
    fun `utbetaling som har feilet mange ganger blir forsøkt på nytt etter en stund`() = runTest {
        val utbetalingsvedtak = ObjectMother.utbetalingsvedtak(
            sendtTilUtbetaling = LocalDateTime.now(fixedClock).minusHours(25),
            status = Utbetalingsstatus.SendtTilOppdrag,
            statusMetadata = Forsøkshistorikk(
                antallForsøk = 10,
                forrigeForsøk = LocalDateTime.now(fixedClock),
            ),
        )
        every { utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() } returns listOf(utbetalingsvedtak)
        coJustRun { utbetalingsklient.iverksett(any(), any(), any()) }
        justRun { utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(utbetalingsvedtak.id, any()) }

        sendUtbetalingerService.send()

        coVerify(exactly = 1) {
            utbetalingsklient.iverksett(any(), any(), any())
        }
    }
}
