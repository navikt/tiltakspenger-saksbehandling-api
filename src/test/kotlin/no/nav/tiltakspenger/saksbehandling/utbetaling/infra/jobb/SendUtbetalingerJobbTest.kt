package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.jobb

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerify
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
import no.nav.tiltakspenger.saksbehandling.infra.setup.MdcKorrelasjon
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelse
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsmigrering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragstilstand
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingProducer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import org.junit.jupiter.api.Test

class SendUtbetalingerJobbTest {
    private val tx = TestSessionFactory.transactionContext
    private val utbetalingRepo = mockk<UtbetalingRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val utbetalingProducer = mockk<UtbetalingProducer>()
    private val statistikkService = mockk<StatistikkService>()
    private val statistikk = mockk<StatistikkDTO>()
    private val utbetaling = ObjectMother.utbetaling(
        beregningKilde = BeregningKilde.BeregningKildeRammebehandling(RammebehandlingId.random()),
    )
    private val sendUtbetalingerJobb = SendUtbetalingerJobb(
        utbetalingRepo = utbetalingRepo,
        meldekortvedtakRepo = mockk<MeldekortvedtakRepo>(),
        utbetalingsklient = utbetalingsklient,
        utbetalingProducer = utbetalingProducer,
        statistikkService = statistikkService,
        sessionFactory = TestSessionFactory(),
        clock = fixedClock,
        korrelasjon = MdcKorrelasjon(),
    )

    init {
        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        every { utbetalingRepo.hentOppdragsversjon(utbetaling.sakId) } returns Oppdragsversjon.V2
        every { utbetalingRepo.låsOppdragstilstand(utbetaling.sakId, any()) } returns Oppdragstilstand(Oppdragsversjon.V2, null)
        coEvery { statistikkService.generer(*anyVararg<Statistikkhendelse>()) } returns statistikk
        justRun { statistikkService.lagre(statistikk, tx) }
        justRun { utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), tx) }
    }

    @Test
    fun `statistikk og markering som sendt skrives i transaksjonen før kallet til helved`() = runTest {
        val sendtUtbetaling = SendtUtbetaling("req", "res", 202, alleredeMottattTidligere = false)
        coEvery { utbetalingsklient.iverksett(any(), any(), any()) } returns Either.Right(sendtUtbetaling)
        justRun { utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, sendtUtbetaling, tx) }

        sendUtbetalingerJobb.sendUtbetalingerTilHelved()

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

        sendUtbetalingerJobb.sendUtbetalingerTilHelved()

        verify(exactly = 1) { utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, kunneIkkeUtbetale, null) }
    }

    @Test
    fun `sak som er sperret eller migrert etter utvalget sendes ikke`() {
        runTest {
            every { utbetalingRepo.låsOppdragstilstand(utbetaling.sakId, any()) } returnsMany listOf(
                Oppdragstilstand(Oppdragsversjon.V2, Oppdragsmigrering.SPERRET),
                Oppdragstilstand(Oppdragsversjon.V3, Oppdragsmigrering.FERDIG),
                Oppdragstilstand(Oppdragsversjon.V2, null),
            )

            sendUtbetalingerJobb.sendUtbetalingerTilHelved()

            sendUtbetalingerJobb.sendUtbetalingerTilHelved()

            verify(exactly = 0) { statistikkService.lagre(statistikk, tx) }
            verify(exactly = 0) { utbetalingRepo.markerSendtTilUtbetaling(any(), any(), any()) }
            coVerify(exactly = 0) { utbetalingsklient.iverksett(any(), any(), any()) }
        }
    }
}
