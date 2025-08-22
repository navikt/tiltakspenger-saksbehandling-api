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
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SendUtbetalingerServiceTest {
    private val meldekortVedtakRepo = mockk<MeldekortVedtakRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val sendUtbetalingerService = SendUtbetalingerService(meldekortVedtakRepo, utbetalingsklient, fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `utbetaling blir iverksatt og markert som sendt til utbetaling`() = runTest {
        val utbetalingsvedtak = ObjectMother.meldekortVedtak()

        every { meldekortVedtakRepo.hentUtbetalingsvedtakForUtsjekk() } returns listOf(utbetalingsvedtak)
        val sendtUtbetaling = SendtUtbetaling("req", "res", 202)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Right(sendtUtbetaling)
        justRun { meldekortVedtakRepo.markerSendtTilUtbetaling(utbetalingsvedtak.id, any(), sendtUtbetaling) }

        sendUtbetalingerService.send()

        verify(exactly = 1) {
            meldekortVedtakRepo.markerSendtTilUtbetaling(
                utbetalingsvedtak.id,
                any(),
                sendtUtbetaling,
            )
        }
    }

    @Test
    fun `feilrespons fra utbetaling lagres`() = runTest {
        val utbetalingsvedtak = ObjectMother.meldekortVedtak()

        every { meldekortVedtakRepo.hentUtbetalingsvedtakForUtsjekk() } returns listOf(utbetalingsvedtak)
        val kunneIkkeUtbetale = KunneIkkeUtbetale("req", "res", 409)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Left(kunneIkkeUtbetale)
        justRun {
            meldekortVedtakRepo.lagreFeilResponsFraUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                utbetalingsrespons = kunneIkkeUtbetale,
            )
        }

        sendUtbetalingerService.send()

        verify(exactly = 1) {
            meldekortVedtakRepo.lagreFeilResponsFraUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                utbetalingsrespons = kunneIkkeUtbetale,
            )
        }
    }
}
