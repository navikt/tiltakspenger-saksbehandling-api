package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

/**
 * Vedtaket er fattet i det saksbehandler iverksetter, så en feilet utbetaling skal ikke holde brevet tilbake.
 * Bruker har krav på vedtaket sitt, og klagefristen løper.
 * Avviket varsles i stedet via metrikk og errorlogg, se [no.nav.tiltakspenger.saksbehandling.infra.metrikker.varsleHvisUtbetalingHarFeilet].
 *
 * Testen kjører isolert fordi `oppdaterUtbetalingsstatus`-jobben sveiper over alle utbetalinger i skjemaet, ikke bare denne sakens.
 * Deler vi skjema med andre route-tester, vil deres jobb hente statusen vår fra deres [no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient] og overskrive den med `Ok`.
 */
class JournalførMeldekortvedtakMedFeiletUtbetalingTest {

    @Test
    @IsolatedDatabaseTest
    fun `journalfører meldekortvedtak selv om utbetalingen har feilet mot oppdrag`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            tac.utbetalingFakeKlient.utbetalingsstatus = Utbetalingsstatus.FeiletMotOppdrag

            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (_, meldekortvedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                // Journalføringen kjøres eksplisitt under, etter at utbetalingsstatusen er hentet.
                jobber = JobberEtterIverksettelse(journalførVedtaksbrev = false),
            )!!

            val repo = tac.utbetalingContext.meldekortvedtakRepo
            repo.hentForVedtakId(meldekortvedtak.id)!!.utbetaling.status shouldBe Utbetalingsstatus.FeiletMotOppdrag

            tac.utbetalingContext.journalførMeldekortvedtakService.journalfør()

            // Brevet skal være journalført til tross for den feilede utbetalingen.
            repo.hentForVedtakId(meldekortvedtak.id)!!.journalpostId.shouldNotBeNull()
        }
    }
}
