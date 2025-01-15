package no.nav.tiltakspenger.utbetaling.service

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.fakes.repos.UtbetalingsvedtakFakeRepo
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.andreMeldekortIverksatt
import no.nav.tiltakspenger.objectmothers.tilSendMeldekortTilBeslutterKommando
import kotlin.test.Test

internal class OpprettUtbetalingsvedtakServiceTest {

    @Test
    fun `neste utbetalingsvedtak peker på forrige`() = runTest {
        with(TestApplicationContext()) {
            val sak = this.andreMeldekortIverksatt()
            val sakId = sak.id
            meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                (sak.meldekortBehandlinger[1] as MeldekortBehandling.IkkeUtfyltMeldekort).tilSendMeldekortTilBeslutterKommando(ObjectMother.saksbehandler()),
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
