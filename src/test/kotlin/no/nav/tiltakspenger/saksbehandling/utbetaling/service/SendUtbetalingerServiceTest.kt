package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelse
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import org.junit.jupiter.api.Test

class SendUtbetalingerServiceTest {
    private val tx = TestSessionFactory.transactionContext
    private val utbetalingRepo = mockk<UtbetalingRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val statistikkService = mockk<StatistikkService>()
    private val statistikk = mockk<StatistikkDTO>()
    private val utbetaling = ObjectMother.utbetaling(
        beregningKilde = BeregningKilde.BeregningKildeRammebehandling(RammebehandlingId.random()),
    )
    private val sendUtbetalingerService = SendUtbetalingerService(
        utbetalingRepo = utbetalingRepo,
        meldekortvedtakRepo = mockk<MeldekortvedtakRepo>(),
        utbetalingsklient = utbetalingsklient,
        statistikkService = statistikkService,
        sessionFactory = TestSessionFactory(),
        clock = fixedClock,
    )

    init {
        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        coEvery { statistikkService.generer(*anyVararg<Statistikkhendelse>()) } returns statistikk
        justRun { statistikkService.lagre(statistikk, tx) }
        justRun { utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), tx) }
    }

    @Test
    fun `statistikk og markering som sendt skrives i transaksjonen før kallet til helved`() = runTest {
        val sendtUtbetaling = SendtUtbetaling("req", "res", 202, alleredeMottattTidligere = false)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Right(sendtUtbetaling)
        justRun { utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, sendtUtbetaling, tx) }

        sendUtbetalingerService.sendUtbetalingerTilHelved()

        coVerifyOrder {
            statistikkService.lagre(statistikk, tx)
            utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), tx)
            utbetalingsklient.iverksett(utbetaling, null, any())
            utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, sendtUtbetaling, tx)
        }
    }

    @Test
    fun `feilrespons fra helved lagres utenfor transaksjonen som rulles tilbake`() = runTest {
        val kunneIkkeUtbetale = KunneIkkeUtbetale(request = "req", feil = ObjectMother.httpKlientUventetStatus(statusCode = 409, body = "res"))
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Left(kunneIkkeUtbetale)
        justRun { utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, kunneIkkeUtbetale, null) }

        sendUtbetalingerService.sendUtbetalingerTilHelved()

        verify(exactly = 1) { utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, kunneIkkeUtbetale, null) }
    }
}
