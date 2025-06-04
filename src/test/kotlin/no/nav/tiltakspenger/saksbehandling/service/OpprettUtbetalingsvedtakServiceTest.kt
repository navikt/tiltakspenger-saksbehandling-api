package no.nav.tiltakspenger.saksbehandling.service

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.andreMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilSendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingsvedtakFakeRepo
import org.junit.jupiter.api.Test

internal class OpprettUtbetalingsvedtakServiceTest {

    @Test
    fun `neste utbetalingsvedtak peker på forrige`() = runTest {
        with(TestApplicationContext()) {
            val sak = this.andreMeldekortIverksatt()
            val sakId = sak.id
            meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                sak.meldekortBehandlinger[1].tilSendMeldekortTilBeslutterKommando(ObjectMother.saksbehandler()),
            )
            meldekortContext.taMeldekortBehandlingService.taMeldekortBehandling(
                meldekortId = sak.meldekortBehandlinger[1].id,
                saksbehandler = ObjectMother.beslutter(),
                correlationId = CorrelationId.generate(),
            )
            meldekortContext.iverksettMeldekortService.iverksettMeldekort(
                IverksettMeldekortKommando(
                    meldekortId = sak.meldekortBehandlinger[1].id,
                    sakId = sakId,
                    beslutter = ObjectMother.beslutter(),
                    correlationId = CorrelationId.generate(),
                ),
            )
            (utbetalingContext.utbetalingsvedtakRepo as UtbetalingsvedtakFakeRepo).hentForSakId(sakId).let {
                it.size shouldBe 2
                it[0].forrigeUtbetalingsvedtakId shouldBe null
                it[1].forrigeUtbetalingsvedtakId shouldBe it[0].id
            }
        }
    }
}
