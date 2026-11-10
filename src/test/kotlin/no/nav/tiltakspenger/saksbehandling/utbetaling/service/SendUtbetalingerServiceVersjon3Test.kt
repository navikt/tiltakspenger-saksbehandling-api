package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.mockk.clearMocks
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
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ProdusertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingProducer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilHelvedUtbetalingJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Egen klasse fordi testklassene deler instans og mocker mellom testene. */
class SendUtbetalingerServiceVersjon3Test {
    private val tx = TestSessionFactory.transactionContext
    private val utbetalingRepo = mockk<UtbetalingRepo>()
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val utbetalingProducer = mockk<UtbetalingProducer>()
    private val statistikkService = mockk<StatistikkService>()
    private val statistikk = mockk<StatistikkDTO>()
    private val utbetaling = ObjectMother.utbetaling(
        beregningKilde = BeregningKilde.BeregningKildeRammebehandling(RammebehandlingId.random()),
    )
    private val sendUtbetalingerService = SendUtbetalingerService(
        utbetalingRepo = utbetalingRepo,
        meldekortvedtakRepo = mockk<MeldekortvedtakRepo>(),
        utbetalingsklient = utbetalingsklient,
        utbetalingProducer = utbetalingProducer,
        statistikkService = statistikkService,
        sessionFactory = TestSessionFactory(),
        clock = fixedClock,
        korrelasjon = MdcKorrelasjon(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(utbetalingRepo, utbetalingsklient, utbetalingProducer, statistikkService)
        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetaling)
        every { utbetalingRepo.hentOppdragsversjon(utbetaling.sakId) } returns Oppdragsversjon.V3
        coEvery { statistikkService.generer(*anyVararg<Statistikkhendelse>()) } returns statistikk
        justRun { statistikkService.lagre(statistikk, tx) }
        justRun { utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), tx) }
        every { utbetalingRepo.hentUtbetalingJson(utbetaling.id) } returns null
    }

    @Test
    fun `sak på versjon 3 lagrer requesten før sending og produserer i sendetransaksjonen`() = runTest {
        val helvedUtbetalingJson = utbetaling.tilHelvedUtbetalingJson(null)
        every {
            utbetalingRepo.lagreUtbetalingJsonHvisMangler(utbetaling.id, any(), tx)
        } returns helvedUtbetalingJson
        justRun { utbetalingProducer.produser(utbetaling.id, any()) }
        justRun { utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, any(), tx) }

        sendUtbetalingerService.sendUtbetalingerTilHelved()

        coVerifyOrder {
            utbetalingRepo.lagreUtbetalingJsonHvisMangler(utbetaling.id, any(), tx)
            statistikkService.lagre(statistikk, tx)
            utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, any(), tx)
            utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, any(), tx)
            utbetalingProducer.produser(utbetaling.id, any())
        }
        coVerify(exactly = 0) { utbetalingsklient.iverksett(any(), any(), any()) }
    }

    @Test
    fun `eksisterende snapshot brukes uten å kjøre mapperen på nytt`() = runTest {
        val utbetalingSomIkkeKanMappes = ObjectMother.utbetaling(
            beregningKilde = BeregningKilde.BeregningKildeRammebehandling(RammebehandlingId.random()),
            satstype = Satstype.MÅNEDLIG,
        )
        val lagretRequest = """{ "body": "lagret uendret" }"""
        every { utbetalingRepo.hentForUtsjekk() } returns listOf(utbetalingSomIkkeKanMappes)
        every { utbetalingRepo.hentOppdragsversjon(utbetalingSomIkkeKanMappes.sakId) } returns Oppdragsversjon.V3
        every { utbetalingRepo.hentUtbetalingJson(utbetalingSomIkkeKanMappes.id) } returns lagretRequest
        justRun { utbetalingRepo.markerSendtTilUtbetaling(utbetalingSomIkkeKanMappes.id, any(), tx) }
        justRun {
            utbetalingRepo.lagreUtbetalingsrespons(
                utbetalingSomIkkeKanMappes.id,
                ProdusertUtbetaling(lagretRequest),
                tx,
            )
        }
        justRun { utbetalingProducer.produser(utbetalingSomIkkeKanMappes.id, lagretRequest) }

        sendUtbetalingerService.sendUtbetalingerTilHelved()

        coVerifyOrder {
            utbetalingRepo.hentUtbetalingJson(utbetalingSomIkkeKanMappes.id)
            utbetalingRepo.lagreUtbetalingsrespons(
                utbetalingSomIkkeKanMappes.id,
                ProdusertUtbetaling(lagretRequest),
                tx,
            )
            utbetalingProducer.produser(utbetalingSomIkkeKanMappes.id, lagretRequest)
        }
        verify(exactly = 0) {
            utbetalingRepo.lagreUtbetalingJsonHvisMangler(any(), any(), any())
        }
    }

    @Test
    fun `manglende utbetaling ved lagring av snapshot produserer ikke på kafka`() = runTest {
        every {
            utbetalingRepo.lagreUtbetalingJsonHvisMangler(utbetaling.id, any(), tx)
        } returns null

        sendUtbetalingerService.sendUtbetalingerTilHelved()

        verify(exactly = 0) { utbetalingProducer.produser(any(), any()) }
        verify(exactly = 0) { statistikkService.lagre(statistikk, tx) }
    }
}
