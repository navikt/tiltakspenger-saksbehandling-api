package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSendForvaltningsmelding
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for de tre køene en opprettholdt klagebehandling går gjennom, jf. testtaksonomien i `AGENTS.md`.
 *
 * Alle tre spørringene velger ut på tvers av alle saker og sorterer eldst først (`order by k.sist_endret`).
 * Testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 *
 * Køene er en kjede: journalføring gjør behandlingen klar for distribusjon, og distribusjon gjør saken klar for oversendelse til klageinstansen.
 * Derfor testes de sammen — hver kø sitt neste steg er den forrige køens utgang.
 * Forvaltningsmeldingen har sine egne to køer, journalføring og distribusjon, som testes på samme måte.
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

    @Test
    @IsolatedDatabaseTest
    fun `forvaltningsmeldingen går gjennom journalføring og distribusjon, eldst først og med limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, eldst) = opprettSakOgSendForvaltningsmelding(tac = tac)!!
            val (_, nyest) = opprettSakOgSendForvaltningsmelding(tac = tac)!!
            // En klagebehandling uten forvaltningsmelding skal aldri havne i køene.
            opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!

            val repo = tac.klagebehandlingContext.klagebehandlingRepo

            repo.hentForvaltningsmeldingerSomSkalJournalføres(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)
            repo.hentForvaltningsmeldingerSomSkalJournalføres(limit = 1).map { it.id } shouldBe listOf(eldst.id)
            repo.hentForvaltningsmeldingerSomSkalDistribueres(limit = 10).shouldBeEmpty()

            tac.klagebehandlingContext.journalførKlagebrevJobb.journalførForvaltningsmeldinger()

            repo.hentForvaltningsmeldingerSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentForvaltningsmeldingerSomSkalDistribueres(limit = 10).map { it.id } shouldBe listOf(eldst.id, nyest.id)
            repo.hentForvaltningsmeldingerSomSkalDistribueres(limit = 1).map { it.id } shouldBe listOf(eldst.id)
            repo.hentForKlagebehandlingId(eldst.id)!!.forvaltningsmelding!!.let {
                it.journalpostId.shouldNotBeNull()
                it.journalføringstidspunkt.shouldNotBeNull()
                it.brevdato.shouldNotBeNull()
                it.brevtekst shouldBe eldst.forvaltningsmelding!!.brevtekst
            }

            tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerForvaltningsmeldinger()

            repo.hentForvaltningsmeldingerSomSkalJournalføres(limit = 10).shouldBeEmpty()
            repo.hentForvaltningsmeldingerSomSkalDistribueres(limit = 10).shouldBeEmpty()
            val distribuert = repo.hentForKlagebehandlingId(eldst.id)!!.forvaltningsmelding!!
            distribuert.distribusjonId.shouldNotBeNull()
            distribuert.distribusjonstidspunkt.shouldNotBeNull()

            // En lagring med en utdatert klagebehandling, lest før jobbene kjørte, skal ikke nullstille journalføringen eller distribusjonen.
            repo.lagreKlagebehandling(eldst)
            repo.hentForKlagebehandlingId(eldst.id)!!.forvaltningsmelding shouldBe distribuert
            repo.hentForvaltningsmeldingerSomSkalJournalføres(limit = 10).shouldBeEmpty()
        }
    }
}
