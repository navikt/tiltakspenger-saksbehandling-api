package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

/**
 * Per-sak-motstykket til `UtbetalingAggregatTest`: den asserter hvilke statuser som holder utbetalingen i køen, denne at statusen leses tilbake på sakens egen utbetaling.
 * Skillet er lesekanalen — køspørringen der, meldekortvedtaket her.
 *
 * `OK_UTEN_UTBETALING` finnes kun i denne retningen: helved svarer det når meldekortet ikke ga penger å utbetale.
 *
 * Testen kjører isolert fordi den styrer en verdi på [no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient] og er avhengig av en jobb som sveiper over alle utbetalinger i skjemaet.
 */
class UtbetalingsstatusRundturTest {

    @Test
    @IsolatedDatabaseTest
    fun `statusen helved svarer med leses tilbake på utbetalingen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, meldekortvedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                // Utbetalingen skal sendes, men statusen skal vi hente selv — én gang per status under.
                jobber = JobberEtterIverksettelse(oppdaterUtbetalingsstatus = false),
            )!!

            fun lagretStatus(): Utbetalingsstatus? =
                tac.utbetalingContext.meldekortvedtakRepo.hentForVedtakId(meldekortvedtak.id)!!.utbetaling.status

            lagretStatus() shouldBe null

            // De to første er ikke endelige, så utbetalingen blir liggende i køen og kan hentes på nytt.
            // `OkUtenUtbetaling` er endelig og må derfor komme sist.
            listOf(
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
                Utbetalingsstatus.OkUtenUtbetaling,
            ).forEach { status ->
                tac.utbetalingFakeKlient.utbetalingsstatus = status
                tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
                lagretStatus() shouldBe status
            }
        }
    }
}
