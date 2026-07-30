package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de to køene på utbetaling, jf. testtaksonomien i `AGENTS.md`.
 *
 * Begge spørringene velger ut på tvers av alle saker og sorterer eldst først.
 * Testene bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 *
 * Merk at et rammevedtak alene ikke gir noen utbetaling — det er meldekortvedtakene som utbetaler.
 * Derfor bygger begge testene en iverksatt meldekortbehandling per sak.
 */
class UtbetalingAggregatTest {

    /**
     * Utsjekkskøen har et kjedekrav i tillegg til utvalget: en utbetaling slipper ikke ut før den forrige utbetalingen på samme sak er sendt *og* kvittert ut med `OK`.
     * Det er dette som holder rekkefølgen mot økonomisystemet, og det er den viktigste delen av kontrakten her.
     */
    @Test
    @IsolatedDatabaseTest
    fun `utsjekkskøen tar usendte utbetalinger, venter på forrige utbetaling på saken, sorterer eldst først og respekterer limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sakA = sakMedUtbetaling(tac)
            val sakB = sakMedUtbetaling(tac)

            val repo = tac.utbetalingContext.utbetalingRepo

            repo.hentForUtsjekk(limit = 10).map { it.sakId } shouldBe listOf(sakA.id, sakB.id)

            // Limit batcher fra toppen av køen, så den eldste utbetalingen kommer først og ingen kan sulte.
            repo.hentForUtsjekk(limit = 1).map { it.sakId } shouldBe listOf(sakA.id)

            // Neste meldekortbehandling på sak A gir en utbetaling som peker på den forrige som sitt forrige ledd.
            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sakA.id,
                kjedeId = sakA.meldeperiodeKjeder[1].kjedeId,
                jobber = JobberEtterIverksettelse.ingen,
            )!!

            // Den nye utbetalingen står på vent: forrige utbetaling på saken er ikke sendt ennå.
            repo.hentForUtsjekk(limit = 10).map { it.sakId } shouldBe listOf(sakA.id, sakB.id)

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

            // Sendt, men ikke kvittert ut: den neste utbetalingen på saken slipper fortsatt ikke til.
            repo.hentForUtsjekk(limit = 10).shouldBeEmpty()

            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

            // Med forrige utbetaling kvittert ut som OK rykker den neste utbetalingen fram i køen.
            repo.hentForUtsjekk(limit = 10).map { it.sakId } shouldBe listOf(sakA.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `statuskøen tar kun sendte utbetalinger som ikke er gjort opp, og tømmes når statusen er hentet`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sakA = sakMedUtbetaling(tac)
            val sakB = sakMedUtbetaling(tac)

            val repo = tac.utbetalingContext.utbetalingRepo

            // Ingenting er sendt ennå, så det finnes ingen status å hente.
            repo.hentDeSomSkalHentesUtbetalingsstatusFor(limit = 10).shouldBeEmpty()

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

            repo.hentDeSomSkalHentesUtbetalingsstatusFor(limit = 10).map { it.sakId } shouldBe listOf(sakA.id, sakB.id)
            repo.hentDeSomSkalHentesUtbetalingsstatusFor(limit = 1).map { it.sakId } shouldBe listOf(sakA.id)

            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

            // En utbetaling med endelig status forlater køen, så jobben ikke spør om den igjen.
            repo.hentDeSomSkalHentesUtbetalingsstatusFor(limit = 10).shouldBeEmpty()
        }
    }

    /**
     * Statuskøen skiller på om statusen er endelig eller ikke.
     * `Ok` og `OkUtenUtbetaling` er endelige og tar utbetalingen ut av køen; de øvrige betyr «ikke ferdig ennå», og da må jobben spørre igjen.
     *
     * Testen styrer en verdi på utbetalingsfaken og er avhengig av en jobb som sveiper over hele skjemaet, og må derfor kjøre isolert.
     */
    @Test
    @IsolatedDatabaseTest
    fun `statuskøen beholder utbetalinger uten endelig status og slipper dem først når statusen er Ok`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val sak = sakMedUtbetaling(tac)
            val repo = tac.utbetalingContext.utbetalingRepo

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

            listOf(
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
                Utbetalingsstatus.FeiletMotOppdrag,
            ).forEach { ikkeEndeligStatus ->
                tac.utbetalingFakeKlient.utbetalingsstatus = ikkeEndeligStatus
                tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

                withClue("$ikkeEndeligStatus er ikke endelig, så utbetalingen må bli stående i køen") {
                    repo.hentDeSomSkalHentesUtbetalingsstatusFor(limit = 10).map { it.sakId } shouldBe listOf(sak.id)
                }
            }

            tac.utbetalingFakeKlient.utbetalingsstatus = Utbetalingsstatus.Ok
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

            repo.hentDeSomSkalHentesUtbetalingsstatusFor(limit = 10).shouldBeEmpty()
        }
    }

    /**
     * Bygger en sak med én usendt utbetaling, gjennom route-laget.
     *
     * Ingen av jobbene kjøres underveis, heller ikke på søknadsbehandlingen.
     * Utbetalingsjobbene sveiper over hele skjemaet, så en jobbkjøring i oppbyggingen av sak nummer to ville sendt utbetalingen på sak nummer én.
     */
    private suspend fun ApplicationTestBuilder.sakMedUtbetaling(tac: TestApplicationContext): Sak {
        val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
        val (oppdatertSak) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            jobber = JobberEtterIverksettelse.ingen,
        )!!
        return oppdatertSak
    }
}
