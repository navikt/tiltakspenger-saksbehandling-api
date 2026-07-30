package no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de tre køene på rammevedtak, jf. testtaksonomien i `AGENTS.md`.
 *
 * Alle tre spørringene velger ut på tvers av alle saker.
 * Derfor bygger testene flere saker og asserter hele køen, uten å filtrere på `sakId`.
 * Rundturen (lagre og hente et rammevedtak) dekkes av e2e-testene og hører ikke hjemme her.
 *
 * Alle tre sorterer eldst først (`order by opprettet`), og testene asserter den rekkefølgen.
 * Sorteringen er det som gjør at `limit` batcher forutsigbart og at ingen vedtak kan sulte i køen.
 */
class RammevedtakAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `journalføringskøen tar kun vedtak uten journalpost som skal ha vedtaksbrev, sorterer eldst først, respekterer limit og tømmes av journalføringsjobben`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattRammevedtak(tac, jobber = JobberEtterIverksettelse.ingen)
            val nyest = iverksattRammevedtak(tac, jobber = JobberEtterIverksettelse.ingen)
            // Kandidaten som skal falle utenfor: saksbehandler har valgt bort vedtaksbrevet.
            iverksattRammevedtak(tac, skalSendeVedtaksbrev = false, jobber = JobberEtterIverksettelse.ingen)

            val repo = tac.behandlingContext.rammevedtakRepo

            // Databasen er isolert og tømt, så køen kan asserteres i sin helhet, i rekkefølge.
            repo.hentRammevedtakSomSkalJournalføres(limit = 10).map { it.id } shouldBe listOf(eldst, nyest)

            // Limit batcher fra toppen av køen, så det eldste vedtaket kommer først og ingen kan sulte.
            repo.hentRammevedtakSomSkalJournalføres(limit = 1).map { it.id } shouldBe listOf(eldst)

            tac.behandlingContext.journalførRammevedtaksbrevService.journalfør()

            // Journalførte vedtak forlater køen, slik at jobben ikke plukker dem opp på nytt.
            repo.hentRammevedtakSomSkalJournalføres(limit = 10).shouldBeEmpty()
        }
    }

    /**
     * Distribusjonskøen er journalføringskøens neste steg: den tar kun vedtak som allerede har journalpost og journalføringstidspunkt.
     * Derfor er et vedtak som ikke er journalført ennå den naturlige kandidaten som skal falle utenfor.
     */
    @Test
    @IsolatedDatabaseTest
    fun `distribusjonskøen tar kun journalførte vedtak som ikke er distribuert, sorterer eldst først og tømmes av distribusjonsjobben`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattRammevedtak(tac, jobber = JobberEtterIverksettelse(distribuerVedtaksbrev = false))
            val nyest = iverksattRammevedtak(tac, jobber = JobberEtterIverksettelse(distribuerVedtaksbrev = false))
            iverksattRammevedtak(tac, jobber = JobberEtterIverksettelse.ingen)

            val repo = tac.behandlingContext.rammevedtakRepo

            repo.hentRammevedtakSomSkalDistribueres(limit = 10).map { it.id } shouldBe listOf(eldst, nyest)
            repo.hentRammevedtakSomSkalDistribueres(limit = 1).map { it.id } shouldBe listOf(eldst)

            tac.behandlingContext.distribuerRammevedtaksbrevService.distribuer()

            repo.hentRammevedtakSomSkalDistribueres(limit = 10).shouldBeEmpty()
        }
    }

    /**
     * Datadelingskøen krever at saken selv er sendt til datadeling.
     * Jobben deler sakene før rammevedtakene i samme kjøring, så ett kall til [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService.send] dekker begge stegene.
     */
    @Test
    @IsolatedDatabaseTest
    fun `datadelingskøen er tom til saken er delt, sorterer eldst først, og tømmes når vedtaket er sendt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattRammevedtak(tac)
            val nyest = iverksattRammevedtak(tac)

            val repo = tac.behandlingContext.rammevedtakRepo

            // Ingen av sakene er delt ennå, så køen er tom selv om vedtakene er iverksatt.
            repo.hentRammevedtakTilDatadeling(limit = 10).shouldBeEmpty()

            tac.sendTilDatadelingService.send()

            // Jobben plukker køen eldst først.
            tac.datadelingFakeKlient.sendteRammevedtak.toList() shouldBe listOf(eldst, nyest)

            // Sendte vedtak forlater køen, så en ny jobbkjøring ikke sender dem på nytt.
            repo.hentRammevedtakTilDatadeling(limit = 10).shouldBeEmpty()
        }
    }

    /**
     * Bygger en sak med et iverksatt rammevedtak gjennom route-laget.
     *
     * @param jobber Slå av jobben som tømmer køen testen ser på — ellers er køen alltid tom.
     */
    private suspend fun ApplicationTestBuilder.iverksattRammevedtak(
        tac: TestApplicationContext,
        skalSendeVedtaksbrev: Boolean = true,
        jobber: JobberEtterIverksettelse = JobberEtterIverksettelse(),
    ): VedtakId {
        val (_, _, rammevedtak) = iverksettSøknadsbehandling(
            tac = tac,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            jobber = jobber,
        )
        return rammevedtak.id
    }
}
