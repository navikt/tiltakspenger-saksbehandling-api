package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import org.junit.jupiter.api.Test

/**
 * Går utbetalingen i stykker mot helved, skal requesten vi sendte lagres på utbetalingen slik at forsøket kan ettergås.
 * Utbetalingen blir liggende i utsjekkskøen og prøves på nytt; at den ikke slipper ut er spørringens kontrakt og asserteres i `UtbetalingAggregatTest`.
 *
 * Testen kjører isolert fordi `sendUtbetalingerTilHelved`-jobben sveiper over alle utbetalinger i skjemaet, ikke bare denne sakens.
 * Deler vi skjema med andre route-tester, ville vår feilende [no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient] feilet deres utbetalinger også.
 */
class SendUtbetalingerMedFeilTest {

    @Test
    @IsolatedDatabaseTest
    fun `feilrespons fra helved lagres på utbetalingen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val feil = KunneIkkeUtbetale(
                request = "requesten vi sendte",
                feil = ObjectMother.httpKlientUventetStatus(statusCode = 409, body = "svaret fra helved"),
            )

            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, meldekortvedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                // Utbetalingen skal ligge igjen i køen, slik at vi selv kan kjøre jobben med en feilende klient.
                jobber = JobberEtterIverksettelse.ingen,
            )!!

            tac.utbetalingFakeKlient.iverksettFeil = feil

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

            tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(
                meldekortvedtak.utbetaling.id,
            ) shouldBe feil.request
        }
    }
}
