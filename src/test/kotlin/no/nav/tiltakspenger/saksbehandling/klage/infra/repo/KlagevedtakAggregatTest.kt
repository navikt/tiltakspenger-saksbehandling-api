package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de to køene på klagevedtak, jf. testtaksonomien i `AGENTS.md`.
 *
 * Begge spørringene velger ut på tvers av alle saker og sorterer eldst først (`order by opprettet`).
 * Testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 *
 * Køene henger sammen: distribusjonskøen tar kun vedtak som allerede er journalført, så de testes i kjede.
 */
class KlagevedtakAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `avvisningsvedtaket går gjennom journalføring og distribusjon, eldst først og med limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, eldst) = opprettSakOgIverksettKlagebehandling(tac = tac, utførJobber = false)!!
            val (_, nyest) = opprettSakOgIverksettKlagebehandling(tac = tac, utførJobber = false)!!

            val repo = tac.klagebehandlingContext.klagevedtakRepo

            repo.hentKlagevedtakSomSkalJournalføres(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)

            // Limit batcher fra toppen av køen, så det eldste vedtaket kommer først og ingen kan sulte.
            repo.hentKlagevedtakSomSkalJournalføres(limit = 1).map { it.id } shouldBe listOf(eldst.id)

            // Distribusjonskøen er tom så lenge vedtaket ikke er journalført.
            repo.hentKlagevedtakSomSkalDistribueres(limit = 10).shouldBeEmpty()

            tac.klagebehandlingContext.journalførKlagebrevJobb.journalførAvvisningbrev()

            repo.hentKlagevedtakSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentKlagevedtakSomSkalDistribueres(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)
            repo.hentKlagevedtakSomSkalDistribueres(limit = 1).map { it.id } shouldBe listOf(eldst.id)

            tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerAvvisningsbrev()

            // Begge køene er tømt, så en ny jobbkjøring ikke gjør det samme på nytt.
            repo.hentKlagevedtakSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentKlagevedtakSomSkalDistribueres(limit = 10).shouldBeEmpty()
        }
    }
}
