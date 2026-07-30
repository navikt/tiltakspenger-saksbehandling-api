package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de tre køene en opprettholdt klagebehandling går gjennom, jf. testtaksonomien i `AGENTS.md`.
 *
 * Alle tre spørringene velger ut på tvers av alle saker og sorterer eldst først (`order by k.sist_endret`).
 * Testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 *
 * Køene er en kjede: journalføring gjør behandlingen klar for distribusjon, og distribusjon gjør saken klar for oversendelse til klageinstansen.
 * Derfor testes de sammen — hver kø sitt neste steg er den forrige køens utgang.
 */
class KlagebehandlingAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `innstillingsbrevet går gjennom journalføring, distribusjon og oversendelse, eldst først og med limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (eldstSak, eldst) = opprettSakOgOpprettholdKlagebehandling(tac = tac, utførJobber = false)!!
            val (nyestSak, nyest) = opprettSakOgOpprettholdKlagebehandling(tac = tac, utførJobber = false)!!

            val repo = tac.klagebehandlingContext.klagebehandlingRepo

            repo.hentInnstillingsbrevSomSkalJournalføres(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)

            // Limit batcher fra toppen av køen, så den eldste behandlingen kommer først og ingen kan sulte.
            repo.hentInnstillingsbrevSomSkalJournalføres(limit = 1).map { it.id } shouldBe listOf(eldst.id)

            // De to neste køene er tomme så lenge brevet ikke er journalført.
            repo.hentInnstillingsbrevSomSkalDistribueres(limit = 10).shouldBeEmpty()
            repo.hentSakerSomSkalOversendesKlageinstansen(limit = 10).shouldBeEmpty()

            tac.klagebehandlingContext.journalførKlagebrevJobb.journalførInnstillingsbrev()

            repo.hentInnstillingsbrevSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentInnstillingsbrevSomSkalDistribueres(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)
            repo.hentInnstillingsbrevSomSkalDistribueres(limit = 1).map { it.id } shouldBe listOf(eldst.id)

            // Oversendelseskøen krever at brevet også er distribuert.
            repo.hentSakerSomSkalOversendesKlageinstansen(limit = 10).shouldBeEmpty()

            tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerInnstillingsbrev()

            repo.hentInnstillingsbrevSomSkalDistribueres(limit = 10).shouldBeEmpty()
            repo.hentSakerSomSkalOversendesKlageinstansen(limit = 10) shouldBe listOf(eldstSak.id, nyestSak.id)
            repo.hentSakerSomSkalOversendesKlageinstansen(limit = 1) shouldBe listOf(eldstSak.id)

            tac.klagebehandlingContext.oversendKlageTilKlageinstansJobb.oversendKlagerTilKlageinstans()

            // Alle tre køene er tømt, så en ny jobbkjøring ikke gjør det samme på nytt.
            repo.hentInnstillingsbrevSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentInnstillingsbrevSomSkalDistribueres(limit = 10).shouldBeEmpty()
            repo.hentSakerSomSkalOversendesKlageinstansen(limit = 10).shouldBeEmpty()
        }
    }
}
