package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.right
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.V3Simuleringsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SimulerServiceOppdragsversjonTest {
    private val utbetalingsklient = mockk<Utbetalingsklient>()
    private val v3Simuleringsklient = mockk<V3Simuleringsklient>()
    private val utbetalingRepo = mockk<UtbetalingRepo>()
    private val navkontorService = mockk<NavkontorService>()
    private val utbetaling = ObjectMother.utbetaling()
    private val forrigeUtbetaling = ObjectMother.utbetaling(
        sakId = utbetaling.sakId,
        saksnummer = utbetaling.saksnummer,
        fnr = utbetaling.fnr,
    )
    private val navkontor = ObjectMother.navkontor()
    private val meldeperiodeKjeder = MeldeperiodeKjeder(emptyList())
    private val resultat = SimuleringMedMetadata(
        simulering = Simulering.IngenEndring(nå(fixedClock)),
        originalResponseBody = "{}",
    )
    private val service = SimulerService(
        utbetalingsklient = utbetalingsklient,
        v3Simuleringsklient = v3Simuleringsklient,
        navkontorService = navkontorService,
        utbetalingRepo = utbetalingRepo,
    )

    @Test
    fun `første og senere simulering i V3-sak bruker direkte V3-klient`() = runTest {
        every { utbetalingRepo.hentOppdragsversjon(utbetaling.sakId) } returns Oppdragsversjon.V3
        every { utbetalingRepo.hentUtbetalingJson(forrigeUtbetaling.id) } returns "forrige-request"
        coEvery {
            v3Simuleringsklient.simuler(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns resultat.right()

        simuler(forrige = null)
        simuler(forrige = forrigeUtbetaling)

        coVerify(exactly = 1) {
            v3Simuleringsklient.simuler(
                sakId = utbetaling.sakId,
                saksnummer = utbetaling.saksnummer,
                behandlingId = any(),
                fnr = utbetaling.fnr,
                beregning = utbetaling.beregning,
                forrigeUtbetalingJson = null,
                meldeperiodeKjeder = meldeperiodeKjeder,
                saksbehandler = utbetaling.saksbehandler,
                brukersNavkontor = navkontor,
            )
        }
        coVerify(exactly = 1) {
            v3Simuleringsklient.simuler(
                sakId = utbetaling.sakId,
                saksnummer = utbetaling.saksnummer,
                behandlingId = any(),
                fnr = utbetaling.fnr,
                beregning = utbetaling.beregning,
                forrigeUtbetalingJson = "forrige-request",
                meldeperiodeKjeder = meldeperiodeKjeder,
                saksbehandler = utbetaling.saksbehandler,
                brukersNavkontor = navkontor,
            )
        }
        coVerify(exactly = 0) { utbetalingsklient.simuler(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { utbetalingRepo.hentUtbetalingJson(utbetaling.id) }
    }

    @Test
    fun `V2-sak fortsetter å bruke den eksisterende utsjekk-klienten`() = runTest {
        every { utbetalingRepo.hentOppdragsversjon(utbetaling.sakId) } returns Oppdragsversjon.V2
        every { utbetalingRepo.hentUtbetalingJson(forrigeUtbetaling.id) } returns "forrige-request"
        coEvery {
            utbetalingsklient.simuler(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns resultat.right()

        simuler(forrige = forrigeUtbetaling)

        coVerify(exactly = 1) {
            utbetalingsklient.simuler(
                sakId = utbetaling.sakId,
                saksnummer = utbetaling.saksnummer,
                behandlingId = any(),
                fnr = utbetaling.fnr,
                saksbehandler = utbetaling.saksbehandler,
                beregning = utbetaling.beregning,
                brukersNavkontor = navkontor,
                kanSendeInnHelgForMeldekort = false,
                forrigeUtbetalingJson = "forrige-request",
                forrigeUtbetalingId = forrigeUtbetaling.id,
                meldeperiodeKjeder = meldeperiodeKjeder,
            )
        }
        coVerify(exactly = 0) { v3Simuleringsklient.simuler(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    private suspend fun simuler(forrige: VedtattUtbetaling?) =
        service.simuler(
            sakId = utbetaling.sakId,
            saksnummer = utbetaling.saksnummer,
            behandlingId = MeldekortId.random(),
            fnr = utbetaling.fnr,
            beregning = utbetaling.beregning,
            forrigeUtbetaling = forrige,
            meldeperiodeKjeder = meldeperiodeKjeder,
            saksbehandler = utbetaling.saksbehandler,
            kanSendeInnHelgForMeldekort = false,
            brukersNavkontor = { navkontor },
        )
}
