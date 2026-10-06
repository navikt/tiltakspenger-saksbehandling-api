package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de tre køene på meldekortvedtak, jf. testtaksonomien i `AGENTS.md`.
 *
 * Alle spørringene velger ut på tvers av alle saker.
 * Derfor bygger testene flere saker og asserter på hele køen, uten å filtrere på `sakId`.
 * Rundturen (lagre og hente et meldekortvedtak) dekkes av e2e-testene og hører ikke hjemme her.
 *
 * Alle køene sorterer eldst først (`order by v.opprettet`), og testene asserter den rekkefølgen.
 * Sorteringen er det som gjør at `limit` batcher forutsigbart og at ingen vedtak kan sulte i køen.
 */
class MeldekortvedtakAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `journalføringskøen tar kun vedtak uten journalpost som skal ha vedtaksbrev, sorterer eldst først, respekterer limit og tømmes av markerJournalført`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattMeldekortvedtak(tac, journalførVedtaksbrev = false)
            val nyest = iverksattMeldekortvedtak(tac, journalførVedtaksbrev = false)
            // Kandidaten som skal falle utenfor: saksbehandler har valgt bort vedtaksbrevet.
            iverksattMeldekortvedtak(tac, skalSendeVedtaksbrev = false, journalførVedtaksbrev = false)

            val repo = tac.utbetalingContext.meldekortvedtakRepo

            // Databasen er isolert og tømt, så køen kan asserteres i sin helhet, i rekkefølge.
            repo.hentDeSomSkalJournalføres(limit = 10).map { it.id } shouldBe listOf(eldst, nyest)

            // Limit batcher fra toppen av køen, så det eldste vedtaket kommer først og ingen kan sulte.
            repo.hentDeSomSkalJournalføres(limit = 1).map { it.id } shouldBe listOf(eldst)

            repo.markerJournalført(
                vedtakId = eldst,
                journalpostId = JournalpostId("journalpost-for-$eldst"),
                tidspunkt = nå(tac.clock),
            )

            // Journalførte vedtak forlater køen, slik at jobben ikke plukker dem opp på nytt.
            repo.hentDeSomSkalJournalføres(limit = 10).map { it.id } shouldBe listOf(nyest)
        }
    }

    /**
     * Datadelingskøen krever både at vedtaket er journalført og at saken selv er sendt til datadeling.
     * Jobben deler sakene før meldekortvedtakene i samme kjøring, så ett kall til [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService.send] dekker begge stegene.
     */
    @Test
    @IsolatedDatabaseTest
    fun `datadelingskøen tar kun journalførte vedtak på delte saker, sorterer eldst først, og tømmes når vedtaket er sendt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattMeldekortvedtak(tac)
            val nyest = iverksattMeldekortvedtak(tac)
            val ikkeJournalført = iverksattMeldekortvedtak(tac, journalførVedtaksbrev = false)

            val repo = tac.utbetalingContext.meldekortvedtakRepo

            // Ingen av sakene er delt ennå, så køen er tom selv om det finnes journalførte vedtak.
            repo.hentMeldekortvedtakTilDatadeling(limit = 10).shouldBeEmpty()

            tac.sendTilDatadelingService.send()

            // Jobben plukker køen eldst først, og det ikke journalførte vedtaket kom aldri inn i den.
            val sendte = tac.datadelingFakeKlient.sendteMeldekortvedtak.toList()
            sendte shouldBe listOf(eldst, nyest)
            sendte shouldNotContain ikkeJournalført

            // Sendte vedtak forlater køen, så en ny jobbkjøring ikke sender dem på nytt.
            repo.hentMeldekortvedtakTilDatadeling(limit = 10).shouldBeEmpty()
        }
    }

    /**
     * Notatkøen tar kun vedtak der saksbehandler aktivt har valgt å journalføre notatet.
     * Både vedtak der notatet er valgt bort og vedtak der notatet allerede er journalført skal falle utenfor.
     */
    @Test
    @IsolatedDatabaseTest
    fun `notatkøen tar kun vedtak som skal ha notat uten journalpost, sorterer eldst først, respekterer limit og tømmes av jobben`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattMeldekortvedtak(tac, skalJournalføreNotat = true)
            val nyest = iverksattMeldekortvedtak(tac, skalJournalføreNotat = true)
            val valgtBort = iverksattMeldekortvedtak(tac, skalJournalføreNotat = false)

            val repo = tac.utbetalingContext.meldekortvedtakRepo

            repo.hentMeldekortvedtakIderMedNotatSomSkalJournalføres(limit = 10) shouldBe listOf(eldst, nyest)
            repo.hentMeldekortvedtakIderMedNotatSomSkalJournalføres(limit = 1) shouldBe listOf(eldst)

            tac.journalførJournalnotatService.journalførNotater()

            repo.hentMeldekortvedtakIderMedNotatSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentForVedtakId(eldst)!!.journalføringsnotat.shouldNotBeNull()
            repo.hentForVedtakId(nyest)!!.journalføringsnotat.shouldNotBeNull()
            repo.hentForVedtakId(valgtBort)!!.journalføringsnotat.shouldBeNull()
        }
    }

    /**
     * Bygger en sak med et iverksatt meldekortvedtak gjennom route-laget.
     *
     * @param journalførVedtaksbrev Sett `false` for å la vedtaket bli liggende i journalføringskøen.
     */
    private suspend fun ApplicationTestBuilder.iverksattMeldekortvedtak(
        tac: TestApplicationContext,
        skalSendeVedtaksbrev: Boolean = true,
        journalførVedtaksbrev: Boolean = true,
        skalJournalføreNotat: Boolean = false,
    ): VedtakId {
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            // Notatjobben tar hele køen, så den må være av også her, ellers tømmer den køen for de forrige meldekortvedtakene.
            jobber = JobberEtterIverksettelse(journalførNotat = false),
        )
        val (_, meldekortvedtak) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            // Fritekst settes for at repoets mapping av tekst_til_vedtaksbrev skal kjøres.
            tekstTilVedtaksbrev = "Fritekst til vedtaksbrevet",
            begrunnelse = "Begrunnelse for meldekortbehandlingen",
            skalJournalføreNotat = skalJournalføreNotat,
            // Notatjobben slås av, slik at vedtak som skal ha notat blir liggende i køen.
            jobber = JobberEtterIverksettelse(journalførVedtaksbrev = journalførVedtaksbrev, journalførNotat = false),
        )!!
        return meldekortvedtak.id
    }
}
