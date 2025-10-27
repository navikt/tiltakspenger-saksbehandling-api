package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.andreMeldekortOpprettet
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilSendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakFakeRepo
import org.junit.jupiter.api.Test

internal class IverksettMeldekortServiceTest {

    @Test
    fun `neste utbetaling peker på forrige`() = runTest {
        with(TestApplicationContext()) {
            val sak = this.andreMeldekortOpprettet()
            val sakId = sak.id
            meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                sak.meldekortbehandlinger[1].tilSendMeldekortTilBeslutterKommando(ObjectMother.saksbehandler()),
            )
            meldekortContext.taMeldekortBehandlingService.taMeldekortBehandling(
                sakId = sakId,
                meldekortId = sak.meldekortbehandlinger[1].id,
                saksbehandler = ObjectMother.beslutter(),
            )
            meldekortContext.iverksettMeldekortService.iverksettMeldekort(
                IverksettMeldekortKommando(
                    meldekortId = sak.meldekortbehandlinger[1].id,
                    sakId = sakId,
                    beslutter = ObjectMother.beslutter(),
                    correlationId = CorrelationId.generate(),
                ),
            )
            (utbetalingContext.meldekortvedtakRepo as MeldekortvedtakFakeRepo).hentForSakId(sakId).let {
                it.size shouldBe 2
                it[0].utbetaling.forrigeUtbetalingId shouldBe null
                it[1].utbetaling.forrigeUtbetalingId shouldBe it[0].utbetaling.id
            }
        }
    }
}
