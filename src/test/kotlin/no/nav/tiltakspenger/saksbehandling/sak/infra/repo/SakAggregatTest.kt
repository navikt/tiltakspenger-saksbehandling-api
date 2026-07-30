package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendTilMeldekortApiService
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de tre køene på sak, jf. testtaksonomien i `AGENTS.md`.
 *
 * Alle tre spørringene velger ut på tvers av alle saker, og alle tre sorterer eldst først (`order by opprettet`).
 * Testene bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 *
 * Merk at ingen av de tre flaggene (`skal_sendes_til_meldekort_api`, `sendt_til_datadeling`, `skal_sende_meldeperioder_til_datadeling`) er lesbare per sak — verken `Sak` eller `SakDb` eksponerer dem.
 * Køspørringen er dermed eneste lesekanal, og det er nettopp derfor kontrakten deres må asserteres her.
 */
class SakAggregatTest {

    /**
     * Utvalgskriteriet er flagget alene — `where skal_sendes_til_meldekort_api = true`.
     * Flagget settes av alt som endrer noe meldekort-api trenger å vite om: mottatt søknad, startet behandling, iverksettelse, avbrudd og iverksatt meldekortbehandling.
     * En sak som bare har fått en søknad står altså i køen, og det er riktig.
     */
    @Test
    @IsolatedDatabaseTest
    fun `meldekort-api-køen tar saker som er flagget, sorterer eldst først, respekterer limit og tømmes én sak av gangen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (eldst) = iverksettSøknadsbehandling(tac = tac)
            val (midterst) = iverksettSøknadsbehandling(tac = tac)
            // Flagget settes allerede når søknaden mottas, så også en sak uten iverksettelse står i køen.
            val (nyest) = sendSøknadsbehandlingTilBeslutning(tac = tac)

            val repo = tac.sakContext.sakRepo

            repo.hentSakIderForSendingTilMeldekortApi(limit = 10) shouldBe listOf(eldst.id, midterst.id, nyest.id)

            // Limit batcher fra toppen av køen, så den eldste saken kommer først og ingen kan sulte.
            repo.hentSakIderForSendingTilMeldekortApi(limit = 2) shouldBe listOf(eldst.id, midterst.id)

            SendTilMeldekortApiService(
                sakRepo = repo,
                meldekortApiHttpClient = tac.meldekortContext.meldekortApiHttpClient,
            ).sendSak(eldst.id)

            // Sending tar ut nøyaktig den ene saken, og lar resten av køen stå.
            repo.hentSakIderForSendingTilMeldekortApi(limit = 10) shouldBe listOf(midterst.id, nyest.id)
        }
    }

    /**
     * De to datadelingskøene på sak henger sammen: meldeperiodekøen krever at saken selv er delt.
     * Jobben deler sakene før meldeperiodene i samme kjøring, så ett kall til [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService.send] dekker begge stegene, og de testes derfor sammen.
     */
    @Test
    @IsolatedDatabaseTest
    fun `datadelingskøene deler først saken og deretter meldeperiodene, sorterer eldst først og tømmes av jobben`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (eldst) = iverksettSøknadsbehandling(tac = tac)
            val (nyest) = iverksettSøknadsbehandling(tac = tac)

            val repo = tac.sakContext.sakRepo

            // Alle nye saker står i sak-køen; det er `sendt_til_datadeling` som tar dem ut igjen.
            repo.hentSakerTilDatadeling(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)
            repo.hentSakerTilDatadeling(limit = 1).map { it.id } shouldBe listOf(eldst.id)

            // Meldeperiodekøen er tom så lenge sakene ikke er delt, selv om iverksettelsen har satt flagget.
            repo.hentForSendingAvMeldeperioderTilDatadeling(limit = 10).shouldBeEmpty()

            tac.sendTilDatadelingService.send()

            tac.datadelingFakeKlient.sendteSaker.toList() shouldBe listOf(eldst.id, nyest.id)
            // Meldeperiodene ble delt i samme kjøring, ett kall per sak, i samme rekkefølge.
            tac.datadelingFakeKlient.sendteMeldeperioder.map { meldeperioder -> meldeperioder.size }.count() shouldBe 2

            // Begge køene er tømt, så en ny jobbkjøring ikke sender det samme på nytt.
            repo.hentSakerTilDatadeling(limit = 10).shouldBeEmpty()
            repo.hentForSendingAvMeldeperioderTilDatadeling(limit = 10).shouldBeEmpty()
        }
    }
}
