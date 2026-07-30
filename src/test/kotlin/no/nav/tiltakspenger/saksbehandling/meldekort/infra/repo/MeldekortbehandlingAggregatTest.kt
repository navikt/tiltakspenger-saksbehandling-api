package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for datadelingskøen på meldekortbehandling, jf. testtaksonomien i `AGENTS.md`.
 *
 * Spørringen velger ut på tvers av alle saker og sorterer eldst først (`order by m.opprettet`).
 * Testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 */
class MeldekortbehandlingAggregatTest {

    /**
     * Køen krever at saken selv er sendt til datadeling.
     * Jobben deler sakene før meldekortbehandlingene i samme kjøring, så ett kall til [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService.send] dekker begge stegene.
     */
    @Test
    @IsolatedDatabaseTest
    fun `datadelingskøen er tom til saken er delt, sorterer eldst først og tømmes når behandlingen er sendt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eldst = iverksattMeldekortbehandling(tac)
            val nyest = iverksattMeldekortbehandling(tac)

            val repo = tac.meldekortContext.meldekortbehandlingRepo

            // Ingen av sakene er delt ennå, så køen er tom selv om behandlingene er iverksatt.
            repo.hentBehandlingerTilDatadeling(limit = 10).shouldBeEmpty()

            tac.sendTilDatadelingService.send()

            // Jobben deler både rammebehandlinger og meldekortbehandlinger; her ser vi kun på de sistnevnte.
            tac.datadelingFakeKlient.sendteBehandlinger.filter { it == eldst || it == nyest } shouldBe listOf(eldst, nyest)

            // Sendte behandlinger forlater køen, så en ny jobbkjøring ikke sender dem på nytt.
            repo.hentBehandlingerTilDatadeling(limit = 10).shouldBeEmpty()
        }
    }

    /** Bygger en sak med en iverksatt meldekortbehandling gjennom route-laget. */
    private suspend fun ApplicationTestBuilder.iverksattMeldekortbehandling(
        tac: TestApplicationContext,
    ): MeldekortId {
        val (sak) = iverksettSøknadsbehandling(tac = tac)
        val (_, _, meldekortbehandling) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
        )!!
        return meldekortbehandling.id
    }
}
